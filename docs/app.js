angular.module('assessmentApp').controller('ShellCtrl', ['$timeout', function ($timeout) {
  var shell = this;
  shell.tab = 'problem';

  // Switch tab, then scroll to an anchor once the (ng-if'd) DOM for that
  // tab has been compiled. Used by every cross-link badge in both tabs.
  shell.goTo = function (tab, anchorId) {
    shell.tab = tab;
    $timeout(function () {
      var el = document.getElementById(anchorId);
      if (el) { el.scrollIntoView({ behavior: 'smooth', block: 'start' }); }
    }, 30);
  };
}]);

angular.module('assessmentApp').controller('MainCtrl', ['DOC_DATA', 'DESIGN_DATA', function (DOC_DATA, DESIGN_DATA) {
  var vm = this;

  vm.meta = DOC_DATA.meta;
  vm.root = DOC_DATA.root;

  // Top-level epic node (level 1) and its intro notes (items on root)
  vm.introItems = DOC_DATA.root.items;
  vm.epic = DOC_DATA.root.children[0];

  vm.sectionId = function (node) {
    return 'sec-' + (node.title || 'untitled').replace(/[^a-zA-Z0-9]+/g, '-').toLowerCase();
  };

  // Reverse traceability: sectionAnchor -> [{usId}] so every requirement
  // section can show which User Story / design artifact covers it.
  vm.designLinksBySection = {};
  DESIGN_DATA.requirements.forEach(function (req) {
    DESIGN_DATA.userStories.forEach(function (us) {
      if (us.linkedRequirements.indexOf(req.id) === -1) { return; }
      var key = req.sectionAnchor;
      vm.designLinksBySection[key] = vm.designLinksBySection[key] || [];
      var exists = vm.designLinksBySection[key].some(function (l) { return l.usId === us.id; });
      if (!exists) { vm.designLinksBySection[key].push({ usId: us.id }); }
    });
  });
}]);

angular.module('assessmentApp').controller('DesignCtrl', ['DESIGN_DATA', function (DESIGN_DATA) {
  var dvm = this;

  dvm.requirements = DESIGN_DATA.requirements;
  dvm.userStories = DESIGN_DATA.userStories;
  dvm.hld = DESIGN_DATA.diagrams.hld;
  dvm.lld = DESIGN_DATA.diagrams.lld;
  dvm.flows = DESIGN_DATA.diagrams.flows;
  dvm.sequence = DESIGN_DATA.diagrams.sequence;
  dvm.decisions = DESIGN_DATA.decisions;
  dvm.bddPlan = DESIGN_DATA.bddPlan;

  dvm.usById = {};
  DESIGN_DATA.userStories.forEach(function (us) { dvm.usById[us.id] = us; });

  dvm.reqById = {};
  DESIGN_DATA.requirements.forEach(function (r) { dvm.reqById[r.id] = r; });

  // Full traceability matrix, derived live from the US <-> requirement <->
  // diagram links above (never hand-maintained, so it can't go stale).
  dvm.traceability = DESIGN_DATA.requirements.map(function (req) {
    var stories = DESIGN_DATA.userStories.filter(function (us) {
      return us.linkedRequirements.indexOf(req.id) !== -1;
    });
    var diagramIds = [];
    stories.forEach(function (us) {
      us.linkedDiagrams.forEach(function (d) {
        if (diagramIds.indexOf(d) === -1) { diagramIds.push(d); }
      });
    });
    return { requirement: req, userStories: stories, diagramIds: diagramIds };
  });
}]);

// Renders a Mermaid diagram from a string expression without ever letting
// the browser's HTML parser see the raw source (class-diagram stereotypes
// like <<interface>> would otherwise be parsed as bogus tags).
angular.module('assessmentApp').directive('mermaidDiagram', ['$timeout', function ($timeout) {
  return {
    restrict: 'A',
    scope: { code: '=mermaidDiagram' },
    link: function (scope, element) {
      function render(code) {
        if (!code) { return; }
        element.empty();
        var id = 'mmd-' + Math.random().toString(36).slice(2);
        var pre = document.createElement('pre');
        pre.className = 'mermaid';
        pre.id = id;
        pre.textContent = code;
        element.append(pre);
        $timeout(function () {
          try {
            window.mermaid.run({ nodes: [document.getElementById(id)] });
          } catch (e) {
            element.append('<div class="text-danger small mt-1">Diagram render error: ' + e.message + '</div>');
          }
        }, 0);
      }
      scope.$watch('code', render);
    }
  };
}]);

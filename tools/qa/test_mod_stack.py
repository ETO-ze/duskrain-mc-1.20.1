"""Supply-chain failure cases: corrupt artifacts, graph conflicts and unsafe filenames."""
import sys
from pathlib import Path
import unittest
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from mod_stack import check_graph, choose, verify, digests, safe_filename


class SupplyChainTests(unittest.TestCase):
    def entry(self,pid,side=('client',),deps=(),default=True):
        return dict(project_id=pid,version_id=pid+'1',file=pid+'.jar',sides=list(side),default=default,dependencies=list(deps))
    def dependency(self,pid,kind='required',version=None):
        return dict(project_id=pid,dependency_type=kind,version_id=version)
    def test_missing_transitive_dependency_rejected(self):
        with self.assertRaises(ValueError):check_graph([self.entry('a',deps=[self.dependency('b')])])
    def test_wrong_side_rejected(self):
        with self.assertRaises(ValueError):check_graph([self.entry('a',('server',),[self.dependency('b')]),self.entry('b')])
    def test_required_version_conflict_rejected(self):
        with self.assertRaises(ValueError):check_graph([self.entry('a',deps=[self.dependency('b',version='b2')]),self.entry('b')])
    def test_incompatible_project_rejected(self):
        with self.assertRaises(ValueError):check_graph([self.entry('a',deps=[self.dependency('b','incompatible')]),self.entry('b')])
    def test_unselected_incompatible_version_allowed(self):
        check_graph([self.entry('a',deps=[self.dependency('b','incompatible','b2')]),self.entry('b')])
    def test_optional_dependency_cannot_satisfy_default(self):
        with self.assertRaises(ValueError):check_graph([self.entry('a',deps=[self.dependency('b')]),self.entry('b',default=False)])
    def test_duplicate_project_rejected(self):
        with self.assertRaises(ValueError):check_graph([self.entry('a'),self.entry('a')])
    def test_traversal_and_sources_rejected(self):
        for name in ('../evil.jar','C:evil.jar','dir\\evil.jar','x-sources.jar','x-dev.jar'):
            with self.subTest(name=name),self.assertRaises(ValueError):safe_filename(name)
    def test_corruption_rejected(self):
        with self.assertRaises(ValueError):verify(b'changed',dict(file='a.jar',**digests(b'original')))
    def test_size_rejected_even_with_correct_hashes(self):
        with self.assertRaises(ValueError):verify(b'good',dict(file='a.jar',size=999,**digests(b'good')))
    def version(self,kind,date,loader='forge',game='1.20.1'):
        return dict(version_type=kind,date_published=date,loaders=[loader],game_versions=[game])
    def test_release_preferred_over_newer_beta(self):
        release=self.version('release','2025');beta=self.version('beta','2026')
        self.assertEqual(choose([beta,release],True),release)
    def test_alpha_never_selected(self):
        with self.assertRaises(ValueError):choose([self.version('alpha','2026')],True)
    def test_wrong_minecraft_or_loader_rejected(self):
        with self.assertRaises(ValueError):choose([self.version('release','2026','fabric'),self.version('release','2026',game='1.21')])


if __name__=='__main__':unittest.main()

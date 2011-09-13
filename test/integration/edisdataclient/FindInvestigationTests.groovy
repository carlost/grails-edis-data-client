package edisdataclient

class FindInvestigationTests extends GroovyTestCase {
  private static final String invNum = '103-006'
  EdisDataService edisDataService

  void testFindInvestigation() {
    def invs = edisDataService.findInvestigations(investigationNumber:invNum)
    assertEquals 1, invs.size()
    def invFound = invs[0]
    assertEquals invNum, invFound.investigationNumber
    assertEquals 'Final', invFound.investigationPhase
    assertEquals 'Inactive', invFound.investigationStatus
    assertEquals 'Probable Effect of Certain Modifications to the North American Free Trade Agreement Rules of Origin, Inv. NAFTA-103-6',
      invFound.investigationTitle
    assertEquals 'Industry and Economic Analysis', invFound.investigationType
  }
}

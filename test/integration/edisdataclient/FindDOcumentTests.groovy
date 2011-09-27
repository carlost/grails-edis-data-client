package edisdataclient

/**
 * User: carlos
 * Date: 9/13/11
 * Time: 9:44 PM
 */
class FindDocumentTests extends GroovyTestCase {

  EdisDataService edisDataService

  void testFindDocument() {
    def invNum = "103-006"
    def invPhase = "Final"
    def docId = 215614L
    def docs = edisDataService.findDocuments(investigationNumber: invNum, investigationPhase: invPhase)

    assertEquals 15, docs.size()
    def docFound = docs.find{docId == it.id} //brittle
    assertNotNull docFound
    println "docFound: ${docFound}"
    assertEquals 'to Zoellick', docFound.documentTitle
    assertEquals 'Public', docFound.securityLevel
    def invFound = docFound.investigation

    //assert for inv data
    assertEquals invNum, invFound.investigationNumber
    assertEquals invPhase, invFound.investigationPhase
    assertEquals 'Inactive', invFound.investigationStatus
    assertEquals 'Probable Effect of Certain Modifications to the North American Free Trade Agreement Rules of Origin, Inv. NAFTA-103-6',
        invFound.investigationTitle
    assertEquals 'Industry and Economic Analysis', invFound.investigationType


    //make sure it doesn't include a value for attachmentListUri
    assertNull invFound.attachmentListUri

    assertEquals 'USITC', docFound.firmOrganization
    assertEquals 'Stephen Koplan', docFound.filedBy
    assertEquals 'Chairman', docFound.onBehalfOf
    
    //these should be dates
    use(StringToDateCategory) {
      assertEquals '2004-09-24 00:00:00.0' as Date, docFound.documentDate
      assertEquals '2004-10-07 00:00:00.0' as Date, docFound.officialReceivedDate
      assertEquals '2011-05-26 09:09:28.0' as Date, docFound.modifiedDate
    }
    //make sure that doc data doesn't duplicate doc data
    assertNull invFound.documentTitle
  }
}

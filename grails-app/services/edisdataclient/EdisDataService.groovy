package edisdataclient

import groovyx.net.http.ContentType
import groovyx.net.http.RESTClient
import org.codehaus.groovy.grails.plugins.codecs.Base64Codec

/**
 *
 * EDIS data webservice interface
 *
 * @author jyankus
 *
 */
class EdisDataService {

  static transactional = false

  private def createRESTClient(params = [:]) {
    def webserviceURL = "https://edis.usitc.gov/data/"
    if (params.baseURL) {
      webserviceURL = params.baseURL
    }
    new RESTClient(webserviceURL)
  }

  /**
   * Generate a secret key for later use
   *
   * Required Arguments:
   * username - the user name
   * password - the password
   *
   * @param params A map of parameters.
   * @return the secretKey
   */
  def secretKey(params = [:]) {
    validateParams(params, ["username", "password"])
    def rest = createRESTClient(params)
    def path = 'secretKey/' + params.username
    def resp = rest.post(path: path, body: [password: params.password], requestContentType: ContentType.URLENC)
    resp.data.secretKey.text()
  }

  /**
   * find investigations.
   *
   * Valid parameters:
   *
   * investigationNumber - the investigation number
   * investigationPhase - the name of the investigation phase
   * investigationType - the name of the investigation type
   * investigationStatus - the name of the investigation status
   *
   * investigationNumber must be provided if investigationPhase is provided
   *
   * @param params the parameters, in map Êform
   * @return
   */
  def findInvestigations(params = [:]) {
    def rest = createRESTClient(params)

    def query = [:]
    if (params.investigationType) {
      query << [investigationType: investigationType]
    }
    if (params.investigationStatus) {
      query << [investigationStatus: investigationStatus]
    }
    def path = "investigation/"
    if (params.investigationNumber) {
      path += params.investigationNumber
      if (params.investigationPhase) {
        path += "/" + params.investigationPhase
      }
    }
    if (params.pageNumber) {
      query << [pageNumber: params.pageNumber]
    }

    def invs = []
    rest.get(contentType: ContentType.XML, path: path) {
      resp, xml ->
      xml.investigations.investigation.each {
        invs << buildInv(it)
      }
    }
    invs
  }

  /**
   * find documents
   *
   * Valid parameters:
   *
   * securityLevel - the security level name
   * investigationNumber - the investigation number
   * investigationPhase - the investigation phase
   * documentType - the document type
   * firmOrg - the firm that filed the doc
   *
   * @return a list of document maps
   */
  def findDocuments(params = [:]) {
    def rest = createRESTClient(params)

    def headers = [:]
    headers << applySecurity(params)

    def query = [:]
    if (params.securityLevel) {
      query << [securityLevel: params.securityLevel]
    }
    if (params.investigationNumber) {
      query << [investigationNumber: params.investigationNumber]
    }
    if (params.investigationPhase) {
      query << [investigationPhase: params.investigationPhase]
    }
    if (params.documentType) {
      query << [documentType: params.documentType]
    }
    if (params.firmOrg) {
      query << [firmOrg: params.firmOrg]
    }
    if (params.pageNumber) {
      query << [pageNumber: params.pageNumber]
    }
    if (params.officialReceivedDate) {
      query << [officialReceivedDate: decodeDateParam(params.officialReceivedDate)]
    }
    if (params.modifiedDate) {
      query << [modifiedDate: decodeDateParam(params.modifiedDate)]
    }

    def path = "document/"
    if (params.id) {
      path = path + params.id
    }

    def resp = rest.get(contentType: ContentType.XML, path: path, query: query, headers: headers)

    def docs = []
    resp.data.documents.document.each {
      docs << buildDoc(it)
    }
    docs
  }

  def findAttachments(params = [:]) {
    validateParams(params, ["documentId"])

    def rest = createRESTClient(params)

    def headers = [:]
    headers << applySecurity(params)

    def atts = []
    rest.get(contentType: ContentType.XML, path: "attachment/" + params.documentId, headers: headers) {
      resp, xml ->
      xml.attachments.attachment.each {
        atts << buildAtt(it)
      }
    }
    atts
  }

  def downloadAttachment(params = [:]) {
    validateParams(params, ["documentId", "attachmentId", "username", "secretKey"])
    def rest = createRESTClient(params)

    def headers = [:]
    headers << applySecurity(params)

    def path = "download/" + params.documentId + "/" + params.attachmentId
    rest.get(contentType: ContentType.BINARY, path: path, headers: headers).data
  }

  private def decodeDateParam(params = [:]) {
    switch (params.comparisonType) {
      case "BETWEEN":
        return params.comparisonType + ":" + params.toDate + ":" + params.fromDate
      case "BEFORE":
      case "AFTER":
      case "EXACT":
        return params.comparisonType + ":" + params.date
      default:
        throw new IllegalArgumentException("Date Parameter values incorrect: $params.comparisonType not a valid comparisonType")
    }

  }

  private def validateParams(params = [:], requiredParams = []) {
    def missingParams = []
    requiredParams.each {
      if (!params.get(it)) {
        missingParams << it
      }
    }
    if (missingParams.size > 0) {
      throw new IllegalArgumentException("Method call missing required parameters $missingParams")
    }
  }

  private def applySecurity(params = [:]) {
    def auth = [:]
    if (params.username && params.secretKey) {
      auth << ["Authorization": "Basic " + new Base64Codec().encode(params.username + ":" + params.secretKey)]
    }
    auth
  }

  private def buildDoc(xml) {
    //everything except for attachmentListUri
    use(StringToDateCategory) {
      def matcher = {name ->
        def results = [true, null]
        if (isInvestigationRelated(name)) {
          results[0] = false
        } else if ('id'.equals(name)) {
          results[1] = Long
        } else if (name =~ /Date$/) {
          results[1] = Date
        }
        results
      }

      def doc = transformXmlToMap(xml, matcher)
      doc << [investigation: buildInv(xml)]
      doc
    }
  }

  private def buildInv(xml) {
    def matcher = { name -> [isInvestigationRelated(name), null]}
    transformXmlToMap xml, matcher
  }

  private def isInvestigationRelated(name) {
    name =~ /^investigation/
  }


  private def transformXmlToMap(xml, matcher) {
    //everything except for documentListUri
    def inv = [:]
    xml.childNodes().each {
      def (include, type) = matcher(it.name)
      if (include) {
        def value = it.text()
        if (type) {
          value = value.asType(type)
        }
        inv << [(it.name): value]
      }
    }
    inv
  }

  private def buildAtt(xml) {
    //everything except for downloadUri
    use(StringToDateCategory) {
      /*
            def matcher = {name ->
        def results = [true, null]
        if (isInvestigationRelated(name)) {
          results[0] = false
        } else if ('id'.equals(name)) {
          results[1] = Long
        } else if (name =~ /Date$/) {
          results[1] = Date
        }
        results
      }

      def doc = transformXmlToMap(xml, matcher)
      doc << [investigation: buildInv(xml)]
      doc
       */
      def att = [:]
      att << [id: xml.id.text()]
      att << [documentId: xml.documentId.text()]
      att << [title: xml.title.text()]
      if (xml.fileSize.text()) {
        att << [fileSize: xml.fileSize.text() as Long]
      }
      att << [originalFileName: xml.originalFileName.text()]
      if (xml.pageCount.text()) {
        att << [pageCount: xml.pageCount.text() as Long]
      }
      att << [createDate: xml.createDate.text() as Date]
      if (xml.lastModifiedDate.text()) {
        att << [lastModifiedDate: xml.lastModifiedDate.text() as Date]
      }
      att
    }
  }

}
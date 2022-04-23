mvn clean verify -Didempiere.target=org.globalqss.idempiere.LCOFE.p2.targetplatform

# NOTE: Compilation is configured for jenkins server at ci.idempiere.org
# if needed to compile locally or in a different server setup you need to change relativePath in files:
#   org.globalqss.idempiere.LCOFE.p2.site/pom.xml
#   org.globalqss.idempiere.LCOFE.p2.targetplatform/pom.xml
#   org.globalqss.idempiere.LCO.electronicinvoice/pom.xml
#   org.globalqss.idempiere.LCOFE-feature/pom.xml
# and the repository location in file:
#   org.globalqss.idempiere.LCOFE.p2.targetplatform/org.globalqss.idempiere.LCOFE.p2.targetplatform.target

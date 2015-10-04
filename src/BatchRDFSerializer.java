import java.awt.List;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;




public class BatchRDFSerializer {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */

	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		File f = new File(args[0]+".ttl");
		if(!(f.exists())) {
			Model bioSystemModel = ModelFactory.createDefaultModel();
			Document BioSystemsDomRoot = basicCalls.openXmlFile(args[0]);
			NodeList systems = BioSystemsDomRoot.getElementsByTagName("System");
			for (int a=0; a<systems.getLength();a++){
				Element BioSystemsDom = ((Element) systems.item(a));
				String bsid = BioSystemsDom.getElementsByTagName("Sys-id_bsid").item(0).getTextContent();
				String bsversion = BioSystemsDom.getElementsByTagName("Sys-id_version").item(0).getTextContent();
				//System.out.println(bsid);
				Resource bioSystemResource = bioSystemModel.createResource("http://www.ncbi.nlm.nih.gov/biosystems/"+bsid);   
				bioSystemResource.addLiteral(DCTerms.identifier, bsid);
				bioSystemResource.addLiteral(Pav.version, bsversion);
				bioSystemResource.addProperty(DC.identifier, bioSystemModel.createResource("http://identifiers.org/biosystems/"+bsid));
				bioSystemResource.addProperty(FOAF.page, BioSystemsDom.getElementsByTagName("System_recordurl").item(0).getTextContent());
				bioSystemResource.addProperty(DCTerms.title, BioSystemsDom.getElementsByTagName("System_names_E").item(0).getTextContent());
				if (BioSystemsDom.getElementsByTagName("System_description").getLength()>0) bioSystemResource.addProperty(DCTerms.description, BioSystemsDom.getElementsByTagName("System_description").item(0).getTextContent());
				NodeList pathwayElements = BioSystemsDom.getElementsByTagName("SysEntity-set");
				for (int j=0; j<pathwayElements.getLength(); j++){
					String pwElementType = ((Element) pathwayElements.item(j)).getElementsByTagName("SysEntity-set_label").item(0).getTextContent();
					//System.out.println(pwElementType);
					NodeList sysEntities = ((Element) pathwayElements.item(j)).getElementsByTagName("SysEntity");
					if (pwElementType.equals("genes and proteins")){
						for (int k=0;k<sysEntities.getLength(); k++){
							String sysLabel = "";
							if (((Element) sysEntities.item(k)).getElementsByTagName("SysEntity_name_E").getLength() > 0) {
								sysLabel = ((Element) sysEntities.item(k)).getElementsByTagName("SysEntity_name_E").item(0).getTextContent();
							}
							String sysId ="";
							if (((Element) sysEntities.item(k)).getElementsByTagName("Sys-gene-molid_E_geneid").getLength() > 0) {
								sysId = ((Element) sysEntities.item(k)).getElementsByTagName("Sys-gene-molid_E_geneid").item(0).getTextContent();
							}
							if (((Element) sysEntities.item(k)).getElementsByTagName("Sys-seq-molid_E_accession").getLength() >0) {
								sysId = ((Element) sysEntities.item(k)).getElementsByTagName("Sys-seq-molid_E_accession").item(0).getTextContent();
							}
							//System.out.println(sysLabel+"\t"+sysId);
							Resource geneProductResource =  bioSystemModel.createResource("http://identifiers.org/ncbigene/"+sysId);
							geneProductResource.addProperty(RDF.type, Wp.GeneProduct);
							geneProductResource.addProperty(DCTerms.isPartOf, bioSystemResource);
							if (sysLabel != "") geneProductResource.addLiteral(RDFS.label, sysLabel);
						}
					}
					if (pwElementType.equals("small molecules")){
						for (int k=0;k<sysEntities.getLength(); k++){
							String sysLabel = "";
							if (((Element) sysEntities.item(k)).getElementsByTagName("SysEntity_name_E").getLength() > 0) {
								sysLabel = ((Element) sysEntities.item(k)).getElementsByTagName("SysEntity_name_E").item(0).getTextContent();
							}
							String sysId = "";
							if (((Element) sysEntities.item(k)).getElementsByTagName("Sys-chem-molid_E_cid").getLength() > 0) {
								sysId = ((Element) sysEntities.item(k)).getElementsByTagName("Sys-chem-molid_E_cid").item(0).getTextContent();
							}
							String extSysId = ((Element) sysEntities.item(k)).getElementsByTagName("Sys-chem-externalrn_externalrn").item(0).getTextContent();
							//System.out.println(sysLabel+"\t"+sysId+"\t"+extSysId);
							Resource smallMoleculeResource = bioSystemModel.createResource("http://identifiers.org/pubchem.compound/"+sysId);
							smallMoleculeResource.addProperty(RDF.type, Wp.Metabolite);
							smallMoleculeResource.addProperty(DCTerms.isPartOf, bioSystemResource);
							smallMoleculeResource.addLiteral(RDFS.label, sysLabel);
						}
					}
					if (pwElementType.equals("Linked Systems")){
						for (int k=0;k<sysEntities.getLength(); k++){
							String linkedDbName = ((Element) sysEntities.item(k)).getElementsByTagName("Dbtag_db").item(0).getTextContent();
							String linkedIdentifier = ((Element) sysEntities.item(k)).getElementsByTagName("Object-id_str").item(0).getTextContent();
							String identifiersOrgUri = null;
							if (linkedDbName.equals("KEGG")){
								identifiersOrgUri = "http://identifiers.org/kegg.pathway/";
							}
							if (linkedDbName.equals("GO")){
								identifiersOrgUri = "http://identifiers.org/go/";
							}
							if (linkedDbName.equals("Reactome")){
								identifiersOrgUri = "http://identifiers.org/reactome/";
							}
							if (linkedDbName.equals("WikiPathways")){
								identifiersOrgUri = "http://identifiers.org/wikipathways/";
							}
							Resource linkedResource = bioSystemModel.createResource(identifiersOrgUri+linkedIdentifier);
							if (identifiersOrgUri==null) System.out.println("PROBLEEM IN LINKED SYSTEM: "+bsid );
							linkedResource.addProperty(RDF.type, Wp.Pathway);
							linkedResource.addProperty(DCTerms.isPartOf, bioSystemResource); //TODO find an appropriate predicate to indicate interacting pathway in contrast to being part of. 
						}
					}

					NodeList pubmedIds = BioSystemsDom.getElementsByTagName("PubMedId");
					for (int l=0; l<pubmedIds.getLength();l++){
						String pubmedId = pubmedIds.item(l).getTextContent();
						Resource pubmedResource = bioSystemModel.createResource("http://identifiers.org/pubmed/"+pubmedId);
						pubmedResource.addProperty(com.hp.hpl.jena.sparql.vocabulary.FOAF.page, bioSystemModel.createResource("http://www.ncbi.nlm.nih.gov/pubmed/"+pubmedId));
						pubmedResource.addProperty(RDF.type, Wp.PublicationReference);
						pubmedResource.addProperty(DCTerms.isPartOf, bioSystemResource);
					}



					if (BioSystemsDom.getElementsByTagName("System_taxid_E").getLength()>0) bioSystemResource.addProperty(Wp.organism, bioSystemModel.createResource("http://purl.obolibrary.org/obo/NCBITaxon_"+
							BioSystemsDom.getElementsByTagName("System_taxid_E").item(0).getTextContent()));
				}
			}
			basicCalls.saveRDF2File(bioSystemModel, args[0]+".ttl", "TURTLE");
		}

	}
}



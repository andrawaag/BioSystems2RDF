import java.awt.List;
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
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;




public class RDFSerializer {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
	public static ArrayList<String> getElementAsString(Document xmlDocument, String elt){
		NodeList elements = xmlDocument.getElementsByTagName(elt);
		ArrayList<String> results = new ArrayList();
		for (int i=0; i<elements.getLength(); i++){
			results.add(elements.item(i).getTextContent());
		}
		return results;
	}
	public static ArrayList<String> getElementAsString(Element xmlDocument, String elt){
		NodeList elements = xmlDocument.getElementsByTagName(elt);
		ArrayList<String> results = new ArrayList();
		for (int i=0; i<elements.getLength(); i++){
			results.add(elements.item(i).getTextContent());
		}
		return results;
	}

	public static void modelGenesAndProteins(Model systemModel, Element element){

	}

	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		Model bioSystemModel = ModelFactory.createDefaultModel();
		Resource bioSystemResource = bioSystemModel.createResource("http://www.ncbi.nlm.nih.gov/biosystems/545294");

		Document BioSystemsDom = basicCalls.openXmlURL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=biosystems&id=545294&rettype=xml&retmode=text");
		for (String element : getElementAsString(BioSystemsDom, "Dbtag_db")){
			System.out.println(element);
		}
		bioSystemResource.addProperty(FOAF.page, BioSystemsDom.getElementsByTagName("System_recordurl").item(0).getTextContent());
		bioSystemResource.addProperty(DCTerms.title, BioSystemsDom.getElementsByTagName("System_names_E").item(0).getTextContent());
		NodeList pathwayElements = BioSystemsDom.getElementsByTagName("SysEntity-set");
		for (int j=0; j<pathwayElements.getLength(); j++){
			String pwElementType = ((Element) pathwayElements.item(j)).getElementsByTagName("SysEntity-set_label").item(0).getTextContent();
			System.out.println(pwElementType);
			NodeList sysEntities = ((Element) pathwayElements.item(j)).getElementsByTagName("SysEntity");
			if (pwElementType.equals("genes and proteins")){
				for (int k=0;k<sysEntities.getLength(); k++){
					String sysLabel = ((Element) sysEntities.item(k)).getElementsByTagName("SysEntity_name_E").item(0).getTextContent();
					String sysId ="";
					if (((Element) sysEntities.item(k)).getElementsByTagName("Sys-gene-molid_E_geneid").getLength() > 0) {
						sysId = ((Element) sysEntities.item(k)).getElementsByTagName("Sys-gene-molid_E_geneid").item(0).getTextContent();
					}
					if (((Element) sysEntities.item(k)).getElementsByTagName("Sys-seq-molid_E_accession").getLength() >0) {
						sysId = ((Element) sysEntities.item(k)).getElementsByTagName("Sys-seq-molid_E_accession").item(0).getTextContent();
					}
					System.out.println(sysLabel+"\t"+sysId);
					Resource geneProductResource =  bioSystemModel.createResource("http://identifiers.org/ncbigene/"+sysId);
					geneProductResource.addProperty(RDF.type, Wp.GeneProduct);
					geneProductResource.addProperty(DCTerms.isPartOf, bioSystemResource);
					geneProductResource.addLiteral(RDFS.label, sysLabel);

				}
			}
			if (pwElementType.equals("small molecules")){
				for (int k=0;k<sysEntities.getLength(); k++){
					String sysLabel = ((Element) sysEntities.item(k)).getElementsByTagName("SysEntity_name_E").item(0).getTextContent();
					String sysId = "";
					if (((Element) sysEntities.item(k)).getElementsByTagName("Sys-chem-molid_E_cid").getLength() > 0) {
						sysId = ((Element) sysEntities.item(k)).getElementsByTagName("Sys-chem-molid_E_cid").item(0).getTextContent();
					}
					String extSysId = ((Element) sysEntities.item(k)).getElementsByTagName("Sys-chem-externalrn_externalrn").item(0).getTextContent();
					System.out.println(sysLabel+"\t"+sysId+"\t"+extSysId);
					Resource smallMoleculeResource = bioSystemModel.createResource("http://identifiers.org/pubchem.compound/"+sysId);
					smallMoleculeResource.addProperty(RDF.type, Wp.Metabolite);
					smallMoleculeResource.addProperty(DCTerms.isPartOf, bioSystemResource);
					smallMoleculeResource.addLiteral(RDFS.label, sysLabel);
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
			
			bioSystemResource.addProperty(Wp.organism, bioSystemModel.createResource("http://purl.obolibrary.org/obo/NCBITaxon_"+
					          BioSystemsDom.getElementsByTagName("System_taxid_E").item(0).getTextContent()));
		}
		basicCalls.saveRDF2File(bioSystemModel, "/tmp/testBioSystems.ttl", "TURTLE");
	}

}



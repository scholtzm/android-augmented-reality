package com.scholtz.aor.util;

import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.scholtz.aor.R;

import android.content.Context;

/**
 * XML Parser class which parses XML database using DOM parser
 * @author Mike
 *
 */
public class XmlParser {
	private final String POIDATA = "poiData";
	private final String POIID = "poiId";
	private final String NAME = "name";
	private final String TYPE = "type";
	private final String DESCRIPTION = "description";
	private final String POSITION = "position";
	private final String LAT = "lat";
	private final String LON = "lon";
	
	private Context context;
	
	public XmlParser(Context context) {
		this.context = context;
	}
	
	public ArrayList<Poi> parse() {
		ArrayList<Poi> pois = new ArrayList<Poi>();
		try {
			InputStream in = context.getResources().openRawResource(R.raw.zastavky);
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = db.parse(in);
			Element root = doc.getDocumentElement();
			NodeList poiList = root.getElementsByTagName(POIDATA);
			
			for(int i = 0; i < poiList.getLength(); i++) {
				Poi p = new Poi();
				Node item = poiList.item(i);
				NodeList properties = item.getChildNodes();
				
				for(int j = 0; j < properties.getLength(); j++) {
					Node property = properties.item(j);
					String name = property.getNodeName();
					
					if(name.equals(POIID)) {
						p.setPoiId(Integer.parseInt(property.getFirstChild().getNodeValue()));
					} else if(name.equals(NAME)) {
						p.setName(property.getFirstChild().getNodeValue());
					} else if(name.equals(TYPE)) {
						p.setType(property.getFirstChild().getNodeValue());
					} else if(name.equals(DESCRIPTION)) {
						if(property.getFirstChild() != null)
							p.setDescription(property.getFirstChild().getNodeValue());
					} else if(name.equals(POSITION)) {
						NodeList coords = property.getChildNodes();
						
						for(int k = 0; k < coords.getLength(); k++) {
							Node coord = coords.item(k);
							
							if(coord.getNodeName().equals(LAT)) {
								p.setLat(Double.parseDouble(coord.getFirstChild().getNodeValue()));
							} else if(coord.getNodeName().equals(LON)) {
								p.setLon(Double.parseDouble(coord.getFirstChild().getNodeValue()));
							}
						}
					}
				}
				pois.add(p);
			}
			
			in.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		return pois;
	}

}

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.hamcrest.core.IsNull;


public class FlowFile {

	BufferedReader ffr;
	Hashtable<String, Sample> individuals;
	Vector<Integer> positions;
	
	public FlowFile(String fileName) throws Exception {
		if (fileName.endsWith(".gz")) {
			ffr = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(fileName))));
		} else {
			ffr = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
		}
	}
	
	public void parseFlow() {
		String line;
		individuals = new Hashtable<String, Sample> ();
		Haplotype matHap = null;
		Haplotype patHap = null;
		
		try {
			while((line=ffr.readLine()) != null) {
				if (!line.isEmpty()) {
					//assume 1 family per file
					if (!line.startsWith("FAMILY")){

						String[] codes = line.trim().split("\\s+");
						String id      = codes[0];
						String status  = codes[1];
						Vector<Segment> seg = findSegments(codes);
						
						if (status.contains("FOUNDER")) {
							if(matHap != null) {
								patHap = new Haplotype(seg);
							} else {
								matHap = new Haplotype(seg);
							}
						} else if (status.contains("MATERNAL")){
							matHap = new Haplotype(seg);
						} else if (status.contains("PATERNAL")) {
							patHap = new Haplotype(seg);
						}
						
						if (matHap != null && patHap != null) {
							individuals.put(id, new Sample(matHap, patHap, id));
							matHap = null;
							patHap = null;
						}
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}			
	}

	private Vector<Segment> findSegments(String[] codes) {
		Vector<Segment> segs = new Vector<Segment> ();
		String last  = codes[2];
		int segStart = getPos(2);
		int segEnd   = getPos(2);
		
		for(int i=3; i<codes.length; i++) {
			String code = codes[i];
			if (code.contentEquals(last)) {
				segEnd = getPos(i);
			} else {
				// store the segment
				segs.add(new Segment(segStart, segEnd, last.getBytes()[0]));
				//reset the values for the next segment
				segStart = getPos(i);
				segEnd   = getPos(i);
			}
			last = code;
		}
		//last segment
		segs.add(new Segment(segStart, segEnd, last.getBytes()[0]));
		return segs;
	}

	private int getPos(int i) {
		//offset for the two extra columns in the flow file id and status
		return positions.get(i-2);
	}

	public Hashtable<String, Sample> getSamples() {
		return individuals;
	}

	public void setPos(Vector<Integer> vector) {
		positions = vector;
	}
	
}

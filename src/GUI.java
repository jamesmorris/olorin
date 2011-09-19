import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.LineBorder;
import pedviz.algorithms.Sugiyama;
import pedviz.graph.Graph;
import pedviz.graph.Node;
import pedviz.loader.CsvGraphLoader;
import pedviz.view.DefaultEdgeView;
import pedviz.view.DefaultNodeView;
import pedviz.view.GraphView;
import pedviz.view.GraphView2D;
import pedviz.view.NodeEvent;
import pedviz.view.NodeListener;
import pedviz.view.NodeView;
import pedviz.view.rules.ColorRule;
import pedviz.view.rules.ShapeRule;
import pedviz.view.symbols.SymbolSexFemale;
import pedviz.view.symbols.SymbolSexMale;
import pedviz.view.symbols.SymbolSexUndesignated;
import java.awt.event.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

public class GUI extends JFrame implements ActionListener {
	
	//FindSegmentsDialog fsd;
        FindSegmentsDialog3 fsd;
	JFileChooser jfc;
	DataDirectory data;
	ProgressMonitor pm;
	JTable variantsTable;
	JPanel pedigreePanel;
	JPanel variantsPanel;
	JPanel segmentPanel;
	JPanel contentPanel;
	JSplitPane mainPane;
	static JTextArea logPanel;	
	private JMenu fileMenu;
	private JMenu toolMenu;
	private JMenuItem openDirectory;
	private JMenuItem exportVariants;
	private JMenuItem exportSegments;
	private JMenuItem findSegments;
	private int minMatches;
	// initialise a global arraylist that will hold all the selected ids from the pedigree
	private Hashtable<String, Integer> filterList = new Hashtable<String, Integer>();
	private ArrayList<Variant> vcfData;
	private ArrayList<SegmentMatch> segments;
	
	FlowFile flow;
	
	
	public static void main(String[] args) {

		new GUI();

	}

	GUI() {
		super("Oberon");
		
		jfc = new JFileChooser("user.dir");
		JMenuBar mb = new JMenuBar();
		
		int menumask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

		fileMenu = new JMenu("File");
		openDirectory = new JMenuItem("Open Directory");
		openDirectory.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,menumask));
		openDirectory.addActionListener(this);
		exportVariants = new JMenuItem("Export Variants");
		exportVariants.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E,menumask));
		exportVariants.addActionListener(this);
		exportVariants.setEnabled(false);
		exportSegments = new JMenuItem("Export Segments");
		exportSegments.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,menumask));
		exportSegments.addActionListener(this);
		exportSegments.setEnabled(false);
		fileMenu.add(openDirectory);
		fileMenu.add(exportVariants);
		fileMenu.add(exportSegments);
		
		mb.add(fileMenu);
		
		toolMenu = new JMenu("Tools");
		findSegments = new JMenuItem("Find Segments");
		findSegments.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,menumask));
		findSegments.addActionListener(this);
		findSegments.setEnabled(false);
		toolMenu.add(findSegments);
		
		mb.add(fileMenu);
		mb.add(toolMenu);
		setJMenuBar(mb);

		pedigreePanel = new JPanel();
		pedigreePanel.setBorder(new LineBorder(Color.BLACK));
		pedigreePanel.setBackground(Color.WHITE);
		pedigreePanel.setLayout(new BorderLayout());
		
		variantsPanel = new JPanel();
		variantsPanel.setBorder(new LineBorder(Color.BLACK));
		variantsPanel.setBackground(Color.WHITE);

        // add a pane next to the log pane that contains a table of the segment including sizes
        // also generate a genome wide plot of the number of chromosome matching 
		segmentPanel = new JPanel();
		segmentPanel.setBorder(new LineBorder(Color.BLACK));
		segmentPanel.setBackground(Color.WHITE);
		
		
		logPanel = new JTextArea();
		logPanel.setEditable(false);
		logPanel.setFont(new Font("Monospaced",Font.PLAIN,12));
		logPanel.setLineWrap(true);
		logPanel.setWrapStyleWord(true);
        JScrollPane loggingPane = new JScrollPane(logPanel);
        loggingPane.setPreferredSize(new Dimension(100,50));
        loggingPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
        //Provide minimum sizes for the components
		Dimension minSizeUpper = new Dimension(100, 50);
		Dimension minSizeLower = new Dimension(100, 50);
		pedigreePanel.setMinimumSize(minSizeUpper);
		variantsPanel.setMinimumSize(minSizeUpper);
		loggingPane.setMinimumSize(minSizeLower);
		segmentPanel.setMinimumSize(minSizeLower);
		//Provide prefered sizes for the components
		Dimension prefSizeUpper = new Dimension(800,500);
		Dimension prefSizeLower = new Dimension(800,70);
		pedigreePanel.setPreferredSize(prefSizeUpper);
		variantsPanel.setPreferredSize(prefSizeUpper);
		loggingPane.setPreferredSize(prefSizeLower);
		segmentPanel.setPreferredSize(prefSizeLower);
				
		//Create a split pane with the two scroll panes in it.
		JSplitPane upperPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pedigreePanel, variantsPanel);
		upperPane.setOneTouchExpandable(true);
		upperPane.setDividerLocation(500);

		JSplitPane lowerPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, loggingPane, segmentPanel);
		lowerPane.setOneTouchExpandable(true);
		lowerPane.setDividerLocation(500);
        
        mainPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, upperPane, lowerPane);
        
        contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.add(mainPane);
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});

		this.setContentPane(contentPanel);
		this.pack();
		this.setVisible(true);
	}

	public void actionPerformed(ActionEvent actionEvent) {
		String command = actionEvent.getActionCommand();
		if (command.equals("Open Directory")) {
			jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if (jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				try {
					this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					try {						
						data = new DataDirectory(jfc.getSelectedFile().getAbsolutePath());
						drawPedigree(data.getPed());
						findSegments.setEnabled(true);
					} catch (Exception e) {
						JOptionPane.showMessageDialog(contentPanel, e.getMessage());
					}
				} finally {
					this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				}
			} 
		} else if (command.equals("Export Variants")) {
			JFileChooser fileChooser = new JFileChooser();
	        int option = fileChooser.showSaveDialog(this);
	        if (option == JFileChooser.APPROVE_OPTION) {
	        	String filename = fileChooser.getSelectedFile().getAbsolutePath();
	        	FileOutputStream fos = null;
				try {
					fos = new FileOutputStream(filename);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        	PrintStream output = new PrintStream(fos);
	        	
	        	output.print("CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER");		
	    		for (String s : fsd.getSelectedInfo()) {
	    			output.print("\t" + s);
	    		}
	    		output.println();
	    		
	    		for (Variant v : vcfData) {
	    			output.println(join(v.getArray(fsd.getSelectedInfo()), "\t" ));
	    		}
	            output.close();
	        }
		} else if (command.equals("Export Segments")) {
			JFileChooser fileChooser = new JFileChooser();
	        int option = fileChooser.showSaveDialog(this);
	        if (option == JFileChooser.APPROVE_OPTION) {
	        	String filename = fileChooser.getSelectedFile().getAbsolutePath();
	        	System.out.println(filename);
	        	FileOutputStream fos = null;
				try {
					fos = new FileOutputStream(filename);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        	PrintStream output = new PrintStream(fos);
	    		for (SegmentMatch s : segments) {
	    			output.println(join(s.getArray(), "\t" ));
	    		}
	            output.close();
	        }
		} else if (command.equals("Find Segments")) {
			ArrayList<Sample> selected = new ArrayList<Sample> ();
			Enumeration<String> e = filterList.keys();
			while (e.hasMoreElements()) {
				String sample = e.nextElement();
				Sample s = data.samples.get(sample);
				selected.add(s);
			}

			if (selected.size() > 1) {
				fsd = new FindSegmentsDialog3(selected.size(), data.vcf.getMeta());
                                fsd.setModal(true);
				fsd.pack();
				fsd.setVisible(true);
				if (fsd.success()) {
					minMatches = fsd.getMinMatches();
                                        
                                        SampleCompare sc = new SampleCompare();
					segments = sc.compareMulti(selected);
					int baseCount = 0;
					for (SegmentMatch m : segments) {
						if (m.getIds().size() >= minMatches) {
							baseCount +=   m.getEnd() - m.getStart();
						}
					}
					System.out.println("Found " + segments.size() + " regions covering " + baseCount/1000000 + "Mb");
                                        
                                        if (segments.size() > 0) {
						
                                            if (fsd.freqFilter()) {
                                                try {
                                                    vcfData = data.vcf.getVariants(segments, minMatches, new FreqFile("/Users/jm20/work/NetBeansProjects/Oberon_svn/data" + File.separator + fsd.getPop() + ".frq.gz"), fsd.getFreqCutoff());
                                                } catch (Exception ex) {
                                                    Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
                                                }
                                                
                                                if (vcfData != null) {
                                                    variantsTable = new JTable(new vcfTableModel(vcfData, fsd.getSelectedInfo(), fsd.getPop()));
                                                    variantsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                                                    TableColumnAdjuster tca = new TableColumnAdjuster(variantsTable);
                                                    tca.setColumnDataIncluded(true);
                                                    tca.adjustColumns();
                                                    variantsTable.setFillsViewportHeight(true);
                                                    variantsTable.setAutoCreateRowSorter(true);
                                                    JScrollPane scrollPane = new JScrollPane(variantsTable);
                                                    scrollPane.setPreferredSize(variantsPanel.getSize());
                                                    //remove previous tables first
                                                    variantsPanel.removeAll();
                                                    variantsPanel.add(scrollPane);
                                                    variantsPanel.repaint();
                                                    this.pack();
                                                    exportVariants.setEnabled(true);
                                                    exportSegments.setEnabled(true);
                                                }
                                            } else {
                                                try {
							vcfData = data.vcf.getVariants(segments, minMatches);
						} catch (IOException e1) {
							// TODO Auto-generated catch block
						}
                                                if (vcfData != null) {
                                                    variantsTable = new JTable(new vcfTableModel(vcfData, fsd.getSelectedInfo()));
                                                    variantsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                                                    TableColumnAdjuster tca = new TableColumnAdjuster(variantsTable);
                                                    tca.setColumnDataIncluded(true);
                                                    tca.adjustColumns();
                                                    variantsTable.setFillsViewportHeight(true);
                                                    variantsTable.setAutoCreateRowSorter(true);
                                                    JScrollPane scrollPane = new JScrollPane(variantsTable);
                                                    scrollPane.setPreferredSize(variantsPanel.getSize());
                                                    //remove previous tables first
                                                    variantsPanel.removeAll();
                                                    variantsPanel.add(scrollPane);
                                                    variantsPanel.repaint();
                                                    this.pack();
                                                    exportVariants.setEnabled(true);
                                                    exportSegments.setEnabled(true);
                                                }
                                            }
                                            
                                            printLog("found " + vcfData.size() + " variants");
					
                                            	
					} else {
                                            // no segments found
                                            // if a table has already been created then remove it
                                            variantsPanel.removeAll();
                                            //remove any created plots
                                            exportVariants.setEnabled(false);
					}
				} 
			}
		}
	}
	
	private void drawPedigree(String csvFile) {
		
		Graph graph = new Graph();
		CsvGraphLoader loader = new CsvGraphLoader(csvFile, ",");
		loader.setSettings("Id", "Mid", "Fid");
		loader.load(graph);
		
		DefaultNodeView n = new DefaultNodeView();
		n.addHintAttribute("Id");
		n.addHintAttribute("Mid");
		n.addHintAttribute("Fid");
		n.setBorderWidth(0.1f);
		
		DefaultEdgeView e = new DefaultEdgeView();
		e.setWidth(0.0005f);
		e.setColor(new Color(100,100,100));
		e.setAlphaForLongLines(0.2f);
		e.setHighlightedColor(Color.black);
		e.setConnectChildren(true);
		
		Sugiyama s = new Sugiyama(graph, n, e);
		s.run();
		
		GraphView2D view = new GraphView2D(s.getLayoutedGraph());
		view.addRule(new ColorRule("aff", "1",  Color.black));
		view.addRule(new ColorRule("aff", "2",  Color.red));
		view.addRule(new ShapeRule("Sex", "1",  new SymbolSexFemale()));
		view.addRule(new ShapeRule("Sex", "2",  new SymbolSexMale()));
		view.addRule(new ShapeRule("Sex", "-1", new SymbolSexUndesignated()));
		view.setSelectionEnabled(true);
		view.addNodeListener(
				new NodeListener() {
					public void onNodeEvent(NodeEvent event) {
						if (event.getType() == NodeEvent.DESELECTED || event.getType() == NodeEvent.SELECTED) {
							String idSelected = (String) event.getNode().getId();
							GraphView graph = event.getGraphView();
							Node node = event.getNode();
							NodeView nodeView = event.getNodeView();
							if (filterList.containsKey(idSelected)) {
								filterList.remove(idSelected);
								printLog("Removed " + idSelected);
								nodeView.setBorderWidth(0.1f);
								nodeView.setBorderColor(Color.black);
							} else {
								filterList.put(idSelected, 1);
								printLog("Selected" + idSelected);
								//highlight the node
								nodeView.setBorderWidth(1);
								nodeView.setBorderColor(Color.green);
							}
						}
					}
				}	
		);
		pedigreePanel.removeAll();
		pedigreePanel.add(view.getComponent());
		contentPanel.revalidate();
		contentPanel.repaint();
	}
	
	public void printLog(String text){
		logPanel.append(text+"\n");
		logPanel.repaint();
    }
	
    public static String join(Collection s, String delimiter) {
        StringBuffer buffer = new StringBuffer();
        Iterator iter = s.iterator();
        while (iter.hasNext()) {
            buffer.append(iter.next());
            if (iter.hasNext()) {
                buffer.append(delimiter);
            }
        }
        return buffer.toString();
    }
	
}

import java.io.*;
import processing.core.*;
import java.util.*;

//mode -1: invalid input
//mode 0: ready for input
//mode 1: reading cmd prompt
//mode 2: ready for new and displaying previous

public class PacketTracerVisualization2 extends PApplet {
	ArrayList<Hop2>[] Hop2s;
	PImage cloud;
	String userInput = "";
	int mode = 0;
	Button2 submitB, newB;
	int displayMode = 0;
	ArrayList<Hop2> displayedHops = new ArrayList<>();
	String[] website;
	int[][] colors = {{255, 0, 0}, {0, 255, 0}, {255, 0, 255}, {255, 255, 0}, {0, 255, 255}, {0, 0, 0}};

	public static void main(String[] args) {
		PApplet.main("PacketTracerVisualization2");
	}
	
	public void settings() {
		size(1346, 768);
	}

	public void setup() {
		submitB = new Button2(this, 200, 70, 100, 40, "Submit");
		newB = new Button2(this, 305, 70, 100, 40, "New");
		//displayB = new Button2(this, width - 350, 70, 200, 40, "Change Display");
		cloud = loadImage("ciscoJSProject/Cloud.png");
	}
	
	public void draw() {
		if (mode != 1)
			background(200);
		displayHeader();

		//mode = 1 when user enters website
		if (mode == 1) {
			textAlign(CENTER, CENTER);
			for (int j = 0; j < website.length; j++) {
				Hop2s[j].add(new Hop2(this, cloud, 0, new int[3], "source", "---------"));

				if (!read(j)) {
					return;
				}
				fill(200);
				strokeWeight(0);
				rect(0, 300, width, height);
				fill(0);
				rectMode(CENTER);
				imageMode(CENTER);
				strokeWeight(4);
				textSize(10);

				for (int i = 0; i < Hop2s[j].size(); i++) {
					for (int k = 0; k < displayedHops.size(); k++) {
						if (Hop2s[j].get(i).ip.equals(displayedHops.get(k).ip) && !displayedHops.get(k).hostname.contains("hidden")) {
							Hop2s[j].get(i).setXY(displayedHops.get(k).x, displayedHops.get(k).y);
							Hop2s[j].get(i).repeat = true;
							break;
						} 
					}
					if (!Hop2s[j].get(i).repeat) {
						Hop2s[j].get(i).setXY(Hop2s[j].size(), i, Hop2s.length, j, displayMode);
						displayedHops.add(Hop2s[j].get(i));
					}
				}
			}
			mode++;
		}
		
		//mode = 2 after tracert is run
		if (mode == 2) {
			drawConnectionsAndNodes();
			drawDelayKey();
		}
	}
	
	// displays destination bar, user input, and buttons
	public void displayHeader() {
		stroke(0);
		strokeWeight(1);
		rectMode(CORNER);
		fill(255);
		rect(200, 25, width - 350, 40);
		submitB.display(false);
		newB.display(false);
		//displayB.display(false);

		fill(0);
		textAlign(RIGHT, CENTER);
		textSize(20);
		text("Destination: ", 200, 45);
		textAlign(LEFT, CENTER);
		text(" " + userInput, 200, 45);
		strokeWeight(4);
		line(0, 150, width, 150);

		if (mode == -1) {
			textAlign(CENTER, CENTER);
			text("Invalid website address", width / 2, height / 2);
		}
	}

	//reads input from command line
	public boolean read(int i) {
		try {
			Process p0 = Runtime.getRuntime().exec("tracert " + website[i]);
			Process p = Runtime.getRuntime().exec("tracert " + website[i]);
			p.waitFor();

			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			
			//checks if website is valid
			if (input.readLine().contains("Unable to resolve") || input.readLine().contains("Usage: ")) {
				mode = -1;
				return false;
			}

			input.readLine();
			input.readLine();
			
			//parses tracert output
			int[] delays = new int[3];
			String line = input.readLine();
			while (true) {
				System.out.println(line);
				StringTokenizer st = new StringTokenizer(line);
				if (st.countTokens() < 5) {
					break;
				}
				int Hop2Num = Integer.parseInt(st.nextToken());

				for (int j = 0; j < 3; j++) {
					delays[j] = -1; // -1 is packet not received
					String next = st.nextToken();
					if (!next.equals("*")) {
						delays[j] = Integer.parseInt(next);
						st.nextToken();
					}
				}

				String next = st.nextToken();
				String hostName, ip;
				if (!st.hasMoreTokens()) {
					ip = next;
					hostName = "none";
				} else {
					if (next.equals("Request")) {
						hostName = "hidden";
						ip = "-hidden-";
					} else {
						hostName = next;
						ip = st.nextToken();
					}
				}
				if (Hop2s[i].size() > 1 && Hop2s[i].get(Hop2s[i].size() - 1).hostname.equals("hidden")
						&& hostName.equals("hidden")) {
					Hop2s[i].get(Hop2s[i].size() - 1).Hop2Num += ", " + Hop2Num;
				} else {
					Hop2s[i].add(new Hop2(this, cloud, Hop2Num, delays, hostName, ip));
				}
				line = input.readLine();
			}
			input.close();
		} catch (Exception err) {
			err.printStackTrace();
		}
		Hop2s[i].get(0).srcDest = true;

		//gets ip of source
		try {
			Process p2 = Runtime.getRuntime().exec("ipconfig");
			p2.waitFor();
			BufferedReader input2 = new BufferedReader(new InputStreamReader(p2.getInputStream()));
			String line = input2.readLine();
			while (!line.contains("IPv4 Address")) {
				line = input2.readLine();
			}
			Hop2s[i].get(0).ip = "IP: " + line.substring(line.lastIndexOf(" ") + 1, line.length());
		} catch (Exception err) {
			err.printStackTrace();
		}

		Hop2s[i].get(Hop2s[i].size() - 1).srcDest = true;
		Hop2s[i].get(Hop2s[i].size() - 1).hostname = "Host: " + website[i];
		System.out.println("done");
		System.out.println(Hop2s[i]);
		return true;
	}

	public void drawConnectionsAndNodes() {
		int repj = -1;
		int repi = -1;
		for (int j = 0; j < Hop2s.length; j++) {
			for (int i = 0; i < Hop2s[j].size() - 1; i++) {
				stroke(colors[Math.min(j, 5)][0], colors[Math.min(j, 5)][1], colors[Math.min(j, 5)][2]);
				Hop2s[j].get(i).connect(Hop2s[j].get(i + 1), j);
				Hop2s[j].get(i).display();
				if (Hop2s[j].get(i).over()) {
					repi = i;
					repj = j;
				}
			}
			Hop2s[j].get(Hop2s[j].size() - 1).display();
		}
		//redisplays node if mouse is over it
		if (repi != -1)
			Hop2s[repj].get(repi).display();
	}

	//Draws a delay legend
	public void drawDelayKey() {
		stroke(255, 0, 0);
		float maxStroke = 40;
		double maxDelay = Hop2s[0].get(0).maxDelay;
		double minDelay = Hop2s[0].get(0).minDelay;
		float startingPos = 75;
		for (double i = minDelay; i <= maxDelay; i++) {
			strokeWeight(Math.min(maxStroke, (float) (maxDelay / i)));
			startingPos += Math.min(maxStroke / 2, maxDelay / i / 2.0);
			line(startingPos, height - 100, startingPos, height - 100);
		}
		textSize(15);
		textAlign(RIGHT, CENTER);
		text((int) Math.ceil(minDelay) / 2 + " ms", (float) (80 - Math.min(maxStroke / 2, maxDelay / minDelay / 2.0)),
				height - 100);
		textAlign(LEFT, CENTER);
		text((int) maxDelay / 2 + " ms", (float) (startingPos + 5), height - 100);
		rectMode(CORNER);
		noFill();
		stroke(0);
		strokeWeight(4);
		rect(0, height - 150, startingPos + 20 + textWidth((int) maxDelay / 2 + " ms"), 150);
		strokeWeight(4);
	}

	//called after a keyboard key is pressed
	public void keyPressed() {
		if (mode == 0 || mode == -1) {
			if (key == BACKSPACE && userInput.length() != 0) {    //adds backspace functionality
				userInput = userInput.substring(0, userInput.length() - 1);
			} else if (key == ENTER || key == RETURN) {
				background(200);
				mode = 1;
				displayHeader();
				textAlign(CENTER, CENTER);
				text("Loading . . .", width / 2, height / 2);
				website = userInput.split(" ");
				Hop2s = new ArrayList[website.length];
				for (int i = 0; i < website.length; i++) {
					Hop2s[i] = new ArrayList<>();
				}
				submitB.display(true);
			} else if (key != BACKSPACE) {
				userInput += key;
			}
		}
	}

	//called when mouse is clicked
	public void mouseClicked() {
		if (submitB.over()) {
			key = ENTER;
			keyPressed();
		}
		if (newB.over()) {
			reset();
		}
		/*if (displayB.over()) {
			displayMode++;
			if (displayMode > 2) {
				displayMode = 0;
			}
			for (int i = 0; i < Hop2s.size(); i++) {
				Hop2s.get(i).setX(Hop2s.size(), i, displayMode);
				Hop2s.get(i).setY(Hop2s.size(), i, displayMode);
			}
		}*/
	}

	//called when new button is pressed, sets mode back to 0
	public void reset() {
		mode = 0;
		userInput = "";
		background(200);
		for (int i = 0; i < Hop2s.length; i++) {
			if (Hop2s[i].size() > 0) {
				Hop2s[i].get(0).maxDelay = 0;
				Hop2s[i].get(0).minDelay = Double.MAX_VALUE;
				Hop2s[i] = new ArrayList<>();
			}
		}
		displayedHops = new ArrayList<>();
	}
}

class Hop2 {
	double x, y;
	String ip, hostname;
	double avgDelay;
	String Hop2Num;
	PApplet parent;
	boolean srcDest;
	int nodeSize = 50;
	PImage cloud;
	static double maxDelay, minDelay = Double.MAX_VALUE;
	boolean repeat;

	Hop2(PApplet parent, PImage cloud, int Hop2Num, int[] delays, String hostname, String ip) {
		this.parent = parent;
		this.cloud = cloud;
		repeat = false;
		int count = 0;
		for (int i = 0; i < 3; i++) {
			if (delays[i] != -1) {
				count++;
				avgDelay += Math.max(0, delays[i]);
			}
		}
		avgDelay = avgDelay / Math.max(1, count);
		maxDelay = Math.max(maxDelay, avgDelay);
		if (avgDelay != 0)
			minDelay = Math.min(minDelay, avgDelay);

		this.hostname = hostname;
		if (!hostname.equals("hidden"))
			this.hostname = "Host: " + this.hostname;
		ip = ip.substring(1, ip.length() - 1);
		this.ip = "IP: " + ip;
		this.Hop2Num = "Hop: " + Hop2Num;
		srcDest = false;
	}

	public void setXY(int divx, int multx, int divy, int multy, int displayMode) {
		double gapx = (parent.width - 50) / divx;
		x = gapx / 2.0 + gapx * multx + 25;
		
		double gapy = (parent.height - 250) / divy;
		y = gapy * multy + 250;
		
	}
	
	public void setXY(double x, double y) {
		this.x = x;
		this.y = y;
	}


	public void connect(Hop2 other, int offset) {
		if (other.avgDelay != 0.0) {
			parent.strokeWeight(Math.min(30, (float) (maxDelay/other.avgDelay)));
		} else {
			parent.strokeWeight(4);
		}
		parent.line((float) x + offset * 4, (float) y + offset * 4, (float) other.x + offset * 4, (float) other.y + offset * 4);
	}

	public void display() {
		parent.strokeWeight(4);
		if (over())
			parent.textSize(15);
		else
			parent.textSize(10);
		parent.stroke(0);
		parent.textAlign(parent.CENTER, parent.CENTER);
		parent.rectMode(parent.CENTER);
		if (hostname.equals("hidden")) {
			parent.fill(150, 150, 150);
			parent.image(cloud, (float) x, (float) y);
		} else if (srcDest) {
			parent.fill(0, 0, 255);
			parent.rect((float) x, (float) y, nodeSize, nodeSize);
		} else {
			parent.fill(255, 255, 255);
			parent.ellipse((float) x, (float) y, nodeSize, nodeSize);
		}

		parent.fill(255, 255, 255);
		parent.strokeWeight(1);
		if (!over() || hostname.equals("hidden")) {
			if (srcDest)
				parent.rect((float) (x), (float) (y - nodeSize * 1.25),
						Math.max(parent.textWidth(hostname), parent.textWidth(ip)) + 10, 50);
			else
				parent.rect((float) (x), (float) (y - nodeSize * 1.25), parent.textWidth(Hop2Num) + 10, 20);
		} else {
			parent.rect((float) (x), (float) (y - nodeSize * 1.25),
					Math.max(parent.textWidth(hostname), parent.textWidth(ip)) + 10, 70);
		}
		parent.strokeWeight(4);
		parent.fill(0, 0, 0);
		if (srcDest || over() && !hostname.equals("hidden")) {
			parent.text(ip, (float) (x), (float) (y - nodeSize * 1.25 + 17));
			parent.text(hostname, (float) x, (float) (y - nodeSize * 1.25));
			parent.text(Hop2Num, (float) x, (float) (y - nodeSize * 1.25 - 17));
		} else {
			parent.text(Hop2Num, (float) x, (float) (y - nodeSize * 1.25));
		}
	}

	public boolean over() {
		return Math.sqrt(Math.pow(parent.mouseX - x, 2) + Math.pow(parent.mouseY - y, 2)) <= nodeSize / 2.0;

	}

	public String toString() {
		return Hop2Num + "\t" + avgDelay + "\t" + hostname + "\t" + ip + "\n";
	}
}

class Button2 {
	double x, y;
	int w, h;
	String text;
	PApplet parent;

	Button2(PApplet parent, double x, double y, int w, int h, String text) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.text = text;
		this.parent = parent;
	}

	public void display(boolean white) {
		parent.strokeWeight(1);
		parent.rectMode(parent.CORNER);
		parent.textSize(14);
		if (over() && !white)
			parent.fill(150);
		else
			parent.fill(255);
		parent.stroke(0);
		parent.rect((float) x, (float) y, w, h);
		parent.textAlign(parent.CENTER, parent.CENTER);
		parent.fill(0);
		parent.text(text, (float) (x + w / 2.0), (float) (y + h / 2.0));
	}

	public boolean over() {
		return parent.mouseX >= x && parent.mouseX <= x + w && parent.mouseY >= y && parent.mouseY <= y + h;
	}
}

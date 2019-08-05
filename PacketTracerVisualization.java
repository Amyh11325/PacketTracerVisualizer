import java.io.*;
import processing.core.*;
import java.util.*;


public class PacketTracerVisualization extends PApplet {
	static ArrayList<Hop> hops = new ArrayList<>();
	PImage cloud;
	String website = "";
	int mode = 0;                                   //mode -1: invalid input, mode 0: ready for input, mode 1: reading cmd prompt, mode 2: ready for new input and displaying previous
	Button submitB, newB, displayB;
	int displayMode = 0;

	public static void main(String[] args) {
		PApplet.main("PacketTracerVisualization");
	}
	
	public void settings() {
		size(1346, 768);
	}

	public void setup() {
		submitB = new Button(this, 200, 70, 100, 40, "Submit");
		newB = new Button(this, 305, 70, 100, 40, "New");
		displayB = new Button(this, width - 350, 70, 200, 40, "Change Display");
		cloud = loadImage("ciscoJSProject/Cloud.png");
	}
	
	public void draw() {
		if (mode != 1)
			background(200);
		displayHeader();

		if (mode == 1) {
			textAlign(CENTER, CENTER);
			hops.add(new Hop(this, cloud, 0, new int[3], "source", "---------"));

			if (!read()) {
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


			for (int i = 0; i < hops.size(); i++) {
				hops.get(i).setX(hops.size(), i, displayMode);
				hops.get(i).setY(hops.size(), i, displayMode);
			}

			mode++;
		}	
		if (mode == 2) {
			drawConnectionsAndNodes();
			drawDelayKey();
		}
	}
	
	public void displayHeader() {
		stroke(0);
		strokeWeight(1);
		rectMode(CORNER);
		fill(255);
		rect(200, 25, width - 350, 40);
		submitB.display(false);
		newB.display(false);
		displayB.display(false);
		
		fill(0);
		textAlign(RIGHT, CENTER);
		textSize(20);
		text("Destination: ", 200, 45);
		textAlign(LEFT, CENTER);
		text(" " + website, 200, 45);
		strokeWeight(4);
		line(0, 150, width, 150);
		
		if (mode == -1) {
			textAlign(CENTER, CENTER);
			text("Invalid website address", width/2, height/2);
		}
	}

	public boolean read() {
		try {
			Process p0 = Runtime.getRuntime().exec("tracert " + website);
			Process p = Runtime.getRuntime().exec("tracert " + website);
			p.waitFor();

			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			
			if(input.readLine().contains("Unable to resolve") || input.readLine().contains("Usage: ")) {
				mode = -1;
				return false;
			}
			
			input.readLine();
			input.readLine();

			int[] delays = new int[3];
			String line = input.readLine();
			while (true) {
				System.out.println(line);
				StringTokenizer st = new StringTokenizer(line);
				if (st.countTokens() < 5) {
					break;
				}
				int hopNum = Integer.parseInt(st.nextToken());

				for (int i = 0; i < 3; i++) {
					delays[i] = -1; // -1 is packet not received
					String next = st.nextToken();
					if (!next.equals("*")) {
						delays[i] = Integer.parseInt(next);
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
				if (hops.size() > 1 && hops.get(hops.size() - 1).hostname.equals("hidden") && hostName.equals("hidden")) {
					hops.get(hops.size() - 1).hopNum += ", " + hopNum;
				} else {
					hops.add(new Hop(this, cloud, hopNum, delays, hostName, ip));
				}
				line = input.readLine();
			}
			input.close();
		} catch (Exception err) {
			err.printStackTrace();
		}
		hops.get(0).srcDest = true;

		try {
			Process p2 = Runtime.getRuntime().exec("ipconfig");
			p2.waitFor();
			BufferedReader input2 = new BufferedReader(new InputStreamReader(p2.getInputStream()));
			String line = input2.readLine();
			while (!line.contains("IPv4 Address")) {
				line = input2.readLine();
			}
			hops.get(0).ip = "IP: " + line.substring(line.lastIndexOf(" ") + 1, line.length());
		} catch (Exception err) {
			err.printStackTrace();
		}

		hops.get(hops.size() - 1).srcDest = true;
		hops.get(hops.size() - 1).hostname = "Destination: " + website;
		System.out.println("done");
		System.out.println(hops);
		return true;
	}

	public void drawConnectionsAndNodes() {
		int rep = -1;
		for (int i = 0; i < hops.size() - 1; i++) {
			hops.get(i).connect(hops.get(i + 1));
			hops.get(i).display();
			if (hops.get(i).over()) {
				rep = i;
			}
		}
		hops.get(hops.size() - 1).display();
		if (rep != -1)
			hops.get(rep).display();
	}

	public void drawDelayKey() {
		stroke(255, 0, 0);
		float maxStroke = 40;
		double maxDelay = hops.get(0).maxDelay;
		double minDelay = hops.get(0).minDelay;
		float startingPos = 75;
		for (double i = minDelay; i <= maxDelay; i++) {
			strokeWeight(Math.min(maxStroke, (float) (maxDelay / i)));
			startingPos += Math.min(maxStroke / 2, maxDelay / i / 2.0);
			line(startingPos, height - 100, startingPos, height - 100);
		}
		textSize(15);
		textAlign(RIGHT, CENTER);
		text((int) Math.ceil(minDelay) / 2 + " ms", (float) (80 - Math.min(maxStroke / 2, maxDelay / minDelay / 2.0)), height - 100);
		textAlign(LEFT, CENTER);
		text((int) maxDelay / 2 + " ms", (float) (startingPos + 5), height - 100);
		rectMode(CORNER);
		noFill();
		stroke(0);
		strokeWeight(4);
		rect(0, height - 150, startingPos + 20 + textWidth((int)maxDelay/2 + " ms"), 150);
		strokeWeight(4);
	}
	
	public void keyPressed() {
		if (mode == 0 || mode == -1) {
			if (key == BACKSPACE && website.length() != 0) {
				website = website.substring(0, website.length() - 1);
			} else if (key == ENTER || key == RETURN) {
				background(200);
				mode = 1;
				displayHeader();
				textAlign(CENTER, CENTER);
				text("Loading . . .", width/2, height/2);
				submitB.display(true);
			} else if (key != BACKSPACE) {
				website += key;
			}
		}
	}
	public void mouseClicked() {
		if (submitB.over()) {
			key = ENTER;
			keyPressed();
		}
		if (newB.over()) {
			reset();
		}
		if (displayB.over()) {
			displayMode++;
			if (displayMode > 2) {
				displayMode = 0;
			}
			for (int i = 0; i < hops.size(); i++) {
				hops.get(i).setX(hops.size(), i, displayMode);
				hops.get(i).setY(hops.size(), i, displayMode);
			}
		}
	}
	
	public void reset() {
		mode = 0;
		website = "";
		background(200);
		if (hops.size() > 0) {
			hops.get(0).maxDelay = 0;
			hops.get(0).minDelay = Double.MAX_VALUE;
			hops = new ArrayList<>();
		}
	}
}

class Hop {
	double x, y;
	String ip, hostname;
	double avgDelay;
	String hopNum;
	PApplet parent;
	boolean srcDest;
	int nodeSize = 50;
	PImage cloud;
	static double maxDelay, minDelay = Double.MAX_VALUE;

	Hop(PApplet p, PImage cloud, int hopNum, int[] delays, String hostname, String ip) {
		parent = p;
		this.cloud = cloud;
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
		this.hopNum = "Hop: " + hopNum;
		srcDest = false;
	}

	public void setX(int div, int mult, int displayMode) {
		if (displayMode == 0 || displayMode == 1) {
			double gap = (parent.width - 50) / (div);
			x = gap / 2.0 + gap * mult + 25;
		} else if (displayMode == 2) {
			x = Math.random() * (parent.width - 100) + 50;
		}
	}

	public void setY(int div, int mult, int displayMode) {
		if (displayMode == 0) {
			y = parent.height/2.0;
		}
		else if (displayMode == 1) {
			double gap = (parent.height - 250) / (div);
			y = gap / 2.0 + gap * mult + 200;
		} else if (displayMode == 2) {
			y = Math.random() * (parent.height - 250) + 200;
		}
	}

	public void connect(Hop other) {
		if (other.avgDelay != 0.0) {
			parent.strokeWeight(Math.min(30, (float) (maxDelay / other.avgDelay)));
		} else {
			parent.strokeWeight(4);
		}
		parent.stroke(255, 0, 0);
		parent.line((float) x, (float) y, (float) other.x, (float) other.y);
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
				parent.rect((float) (x), (float) (y - nodeSize * 1.25), Math.max(parent.textWidth(hostname), parent.textWidth(ip)) + 10, 50);
			else
				parent.rect((float) (x), (float) (y - nodeSize * 1.25), parent.textWidth(hopNum) + 10, 20);
		}else {
			parent.rect((float) (x), (float) (y - nodeSize * 1.25), Math.max(parent.textWidth(hostname), parent.textWidth(ip)) + 10, 70);
		}
		parent.strokeWeight(4);
		parent.fill(0, 0, 0);
		if (srcDest || over() && !hostname.equals("hidden")) {
			parent.text(ip, (float) (x), (float) (y - nodeSize * 1.25 + 17));
			parent.text(hostname, (float) x, (float) (y - nodeSize * 1.25));
			parent.text(hopNum, (float) x, (float) (y - nodeSize * 1.25 - 17));
		} else {
			parent.text(hopNum, (float) x, (float) (y - nodeSize * 1.25));
		}
	}
	
	public boolean over() {
		return Math.sqrt(Math.pow(parent.mouseX - x, 2) + Math.pow(parent.mouseY - y, 2)) <= nodeSize/2.0;
		
	}

	public String toString() {
		return hopNum + "\t" + avgDelay + "\t" + hostname + "\t" + ip + "\n";
	}
}
class Button {
	double x, y;
	int w, h;
	String text;
	PApplet parent;
	Button(PApplet parent, double x, double y, int w, int h, String text) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h= h;
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
		parent.rect((float)x, (float)y,  w,  h);
		parent.textAlign(parent.CENTER, parent.CENTER);
		parent.fill(0);
		parent.text(text, (float)(x + w/2.0), (float) (y + h/2.0));
	}
	public boolean over() {
		return parent.mouseX >= x && parent.mouseX <= x + w && parent.mouseY >= y && parent.mouseY <= y + h;
	}
}

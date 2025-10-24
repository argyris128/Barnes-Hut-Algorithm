import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class Object {
    double x, y, vx, vy, m;
    String s;
    Quadtree area; // smallest area that contains object

    Object(double x, double y, double vx, double vy, double m, String s) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.m = m;
        this.s = s;
    }
}

class Quadtree {
    Quadtree[] children = new Quadtree[4];
    double x, y; // coordinates of the quadrant
    double size; // size of the quadrant (length)
    int hasObject; // number of objects inside the quadrant
    List<Object> objects = new ArrayList<>(); // array of objects inside in the quadrant
    double cmx, cmy; // center of mass (x,y)

    boolean isLeaf() {
        for (Quadtree child : children) {
            if (child != null)
                return false;
        }
        return true;
    }
}

public class Main implements Runnable {
    static List<Object> objects; // array of all objects
    static int N;
    static double size, G, dt;

    private Quadtree quad;
    private int startIndex, endIndex;

    public Main (Quadtree quad, int startIndex, int endIndex) {
        this.quad = quad;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    public void run() {
        for(int i = startIndex; i < endIndex; i++)
            netForce(quad, objects.get(i));
    }

    static void printFile(FileWriter out) throws IOException {
        out.write(N + "\n");
        out.write(size + "\n");

        for (Object obj : objects) {
            String x = String.format("%.7e", obj.x);
            String y = String.format("%.7e", obj.y);
            String vx = String.format("%.7e", obj.vx);
            String vy = String.format("%.7e", obj.vy);
            String m = String.format("%.7e", obj.m);
            out.write(x + " " + y + " " + vx + " " + vy + " " + m + " " + obj.s + "\n");
        }
    }

    static boolean isInsideQuadrant(Quadtree quad, Object object) {
        double edgex1 = quad.x - quad.size / 2;
        double edgex2 = quad.x + quad.size / 2;
        double edgey1 = quad.y - quad.size / 2;
        double edgey2 = quad.y + quad.size / 2;

        return (object.x >= edgex1 && object.x < edgex2 &&
                object.y >= edgey1 && object.y < edgey2);
    }

    static void objectCount(Quadtree quad) {
        int temp = -1;
        for (int i = 0; i < N; i++) {
            if (isInsideQuadrant(quad, objects.get(i))) {
                quad.objects.add(objects.get(i));
                quad.hasObject++;
                temp = i;
            }
        }

        if(quad.hasObject == 1)
            objects.get(temp).area = quad;
    }

    static void splitSpace(Quadtree quad) {
        objectCount(quad);

        if (quad.hasObject == 0 || quad.hasObject == 1)
            return;

        double quarterSize = quad.size / 4.0;

        for (int i = 0; i < 4; i++) {
            quad.children[i] = new Quadtree();
            quad.children[i].x = quad.x + ((i % 2 == 0) ? -quarterSize : quarterSize);
            quad.children[i].y = quad.y + ((i < 2) ? quarterSize : -quarterSize);
            quad.children[i].size = quad.size / 2.0;
            quad.children[i].hasObject = 0;
            quad.cmx = 0.0;
            quad.cmy = 0.0;

            splitSpace(quad.children[i]);
        }
    }

    static void centerOfMass(Quadtree quad) {
        if (quad == null)
            return;

        for (Quadtree child : quad.children)
            centerOfMass(child);

        if (!quad.isLeaf()) {
            double totalm = 0.0, totalxm = 0.0, totalym = 0.0;

            for (Object obj : quad.objects) {
                totalm += obj.m;
                totalxm += obj.m * obj.x;
                totalym += obj.m * obj.y;
            }

            if (totalm != 0) {
                quad.cmx = totalxm / totalm;
                quad.cmy = totalym / totalm;
            } else {
                quad.cmx = quad.x;
                quad.cmy = quad.y;
            }
        }
    }

    static void netForce(Quadtree quad, Object object) {
        if (quad == null)
            return;

        if (!quad.isLeaf()) {
            double r = Math.sqrt(Math.pow(quad.cmx - object.x, 2) + Math.pow(quad.cmy - object.y, 2));

            if (object.area != null) {
                if (r >= object.area.size && !isInsideQuadrant(quad, object)) { // 1st case
                    for (Object obj : quad.objects) {
                        double F = G * obj.m * object.m / Math.pow(r, 2);
                        double Fx = F * (quad.cmx - object.x) / r;
                        double Fy = F * (quad.cmy - object.y) / r;
                        double ax = Fx / object.m;
                        double ay = Fy / object.m;

                        object.vx += dt * ax;
                        object.vy += dt * ay;
                        object.x += dt * object.vx;
                        object.y += dt * object.vy;
                    }
                    return;
                } else { // 2nd case
                    for (Quadtree child : quad.children)
                        netForce(child, object);
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        try {
            BufferedReader in = new BufferedReader(new FileReader(args[0]));
            FileWriter out = new FileWriter(args[1]);
            int numloops = Integer.parseInt(args[2]);
            int numthreads = Integer.parseInt(args[3]);

            double start = System.nanoTime();

            String line = in.readLine();
            N = Integer.parseInt(line);

            line = in.readLine();
            size = Double.parseDouble(line);

            objects = new ArrayList<>();

            for (int i = 0; i < N; i++) {
                line = in.readLine();
                String[] parts = line.split(" ");
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);
                double vx = Double.parseDouble(parts[2]);
                double vy = Double.parseDouble(parts[3]);
                double m = Double.parseDouble(parts[4]);
                String s = parts[5];
                objects.add(new Object(x, y, vx, vy, m, s));
            }

            Quadtree root = new Quadtree();
            root.x = 0.0;
            root.y = 0.0;
            root.size = size * 2.0;
            root.hasObject = 0;
            root.cmx = 0;
            root.cmy = 0;

            G = 6.67 * Math.pow(10.0, -11.0);
            dt = 1.0;

            for (int j = 0; j < numloops; j++) {
                int chunkSize = N / numthreads;
                int remaining = N % numthreads;
                int startIndex = 0;
                int endIndex;

                splitSpace(root); // serial
                centerOfMass(root); // serial

                for (int i = 0; i < numthreads; i++) {
                    endIndex = startIndex + chunkSize + (i < remaining ? 1 : 0);

                    Thread thread = new Thread(new Main(root, startIndex, endIndex)); // parallel

                    startIndex = endIndex;

                    thread.start();
                    thread.join();
                }
            }

            double end = System.nanoTime();
            double elapsed = (end - start) * Math.pow(10.0, -9.0);;
            String e = String.format("%.6f", elapsed);
            System.out.println("Run time: " + e + "s");
            printFile(out);

            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


import processing.core.PApplet;
import processing.data.JSONObject;
import processing.data.JSONArray;
import java.util.ArrayList;
import java.util.HashMap;
import java.awt.Rectangle;
import java.awt.Point;

public class LapResetContext extends BasicContextList {

    protected ArrayList<Context> contexts;
    protected Point size;
    protected float scale;

    public LapResetContext(TreadmillController tc, float track_length, JSONObject context_info) {
        super(context_info, 0, "");
    }


    public UdpClient getComm() { return null; }
    public void sendCreateMessages() { }
    public boolean setupComms() { return true; }
    public void registerContexts(ArrayList<ContextList> contexts) { }

    public String getId() {
        return this.id;
    }

    public void setSize(Point size) {
        if (size != null) {

            for (Context context : this.contexts) {
                context.setSize(size);
            }
        }

        this.size = size;
        this.setDisplayScale(this.scale);
    }

    public Point getSize() {
        return this.size;
    }

    public float getTrackLength() {
        return this.track_length;
    }

    public void setDisplayScale(float scale) {
        this.scale = scale;
        this.display_size.move(
            (int) (this.size.x * scale), (int) (this.size.y * scale));
    }

    public int[] displayColor() {
        return this.display_color;
    }

    public void setStatus(String status) {
        this.status = status;

        waiting = false;
        this.tries = 0;
    }

    public String getStatus() {
        return this.status;
    }

    public Context getContext(int i) {
        return this.contexts.get(i);
    }

    public void move(int index, Point location) {
        this.contexts.get(index).move(location);
    }

    public void clear() {
        contexts = new ArrayList<Context>();
    }

    public int[][] toList() {
        int[][] list = new int[contexts.size()][4];
        for (int i = 0; i < contexts.size(); i++) {
            Rectangle location = contexts.get(i).location;
            list[i] = new int[]{location.x, location.y, location.width, location.height};
        }

        return list;
    }

    public boolean check(Point position, float time, int lap, int lick_count, HashMap<Integer,
            Integer> sensor_counts, JSONObject[] msg_buffer) {

        boolean inZone = false;
        int i = 0;

        // This loop checks to see if any of the individual contexts are
        // triggered to be active both in space and time
        for (; i < this.contexts.size(); i++) {
            if (this.contexts.get(i).check(position, time, lap)) {
                inZone = true;
                break;
            }
        }

        // Decide if the context defined by this ContextList needs to swtich
        // state and send the message to the UdpClient accordingly
        if (!waiting) {
            if ((!inZone) && (this.active != -1)) {
                this.status = "sent stop";
                this.active = -1;
                this.waiting = true;
                this.sent = time;
                this.sendMessage(this.stopString);
            } else if((inZone) && (this.active != i)) {
                this.active = i;
                this.waiting = true;
                this.sent = time;
                this.status = "sent start";
                this.sendMessage(this.startString);
            }
        }

        // Ensure that the context has actually started and reset if necessary
        if ((this.waiting) && (time-this.sent > 2)) {
            this.tries++;
            if (this.tries > 3) {
                System.out.println("[" + this.id + "] RESET CONTEXT " +
                                   this.tries);
                this.tries = 0;
                sendCreateMessages();
            } else {
                System.out.println("[" + this.id + "] RESEND TO CONTEXT " +
                                   this.tries);
                this.comm.setStatus(false);
                if (!inZone) {
                    this.sent = time;
                    this.sendMessage(this.stopString);
                } else if(inZone) {
                    this.sent = time;
                    this.sendMessage(this.startString);
                }
            }
        }

        return (this.active != -1);
    }


    public void trialStart(JSONObject[] msg_buffer) { }

    public void reset() {
        for (Context context : contexts) {
            context.reset();
        }
    }

    public void end() {
        this.reset();
    }

    public boolean isActive() {
        return this.active != -1;
    }

    public int activeIdx() {
        return this.active;
    }

    public void suspend() {
        this.active = -1;
        this.status = "sent stop";
        this.sendMessage(this.stopString);
    }

    public void stop(float time, JSONObject[] msg_buffer) {
        this.active = -1;
        this.status = "sent stop";
        this.waiting = false;
        this.sendMessage(this.stopString);
    }

    public void sendMessage(String message) { }

}

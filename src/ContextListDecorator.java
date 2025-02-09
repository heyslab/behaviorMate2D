import java.util.ArrayList;
import java.util.HashMap;
import processing.data.JSONObject;
import java.awt.Point;

/**
 * Class for wrapping an instance of a class that implements the <code>ContextList</code> interface
 * and providing additional functionality. All methods simply call the corresponding method of the
 * wrapped <code>ContextList</code>. Subclasses of <code>ContextListDecorator</code> will override
 * some or all of these rather than using the wrapped class' implementation.
 */
public class ContextListDecorator implements ContextList {

    protected ContextList context_list;

    public ContextListDecorator(ContextList context_list) {
        this.context_list = context_list;
    }

    public UdpClient getComm() {
        return this.context_list.getComm();
    }

    public void sendCreateMessages() {
        this.context_list.sendCreateMessages();
    }

    public boolean setupComms(ArrayList<UdpClient> comms) {
        return this.context_list.setupComms(comms);
    }

    public void registerContexts(ArrayList<ContextList> contexts) {
        this.context_list.registerContexts(contexts);
    }

    public String getId() {
        return this.context_list.getId();
    }

    public void setSize(Point size) {
        this.context_list.setSize(size);
    }

    public Point getSize() {
        return this.context_list.getSize();
    }

    public float getTrackLength() {
        return this.context_list.getTrackLength();
    }

    public void setDisplayScale(float scale) {
        this.context_list.setDisplayScale(scale);
    }

    public Point displaySize() {
        return this.context_list.displaySize();
    }

    public int[] displayColor() {
        return this.context_list.displayColor();
    }

    public void setStatus(String status) {
        this.context_list.setStatus(status);
    }

    public String getStatus() {
        return this.context_list.getStatus();
    }

    public int size() {
        return this.context_list.size();
    }

    public Point getLocation(int i) {
        return this.context_list.getLocation(i);
    }

    public Context getContext(int i) {
        return this.context_list.getContext(i);
    }

    public void move(int index, Point location) {
        this.context_list.move(index, location);
    }


    public void clear() {
        this.context_list.clear();
    }

    //public void shuffle() {
    //    this.context_list.shuffle();
    //}

    public int[][] toList() {
        return this.context_list.toList();
    }

    public boolean check(Point position, float time, int lap, int lick_count, HashMap<Integer,
            Integer> sensor_counts, JSONObject[] msg_buffer) {

        return this.context_list.check(position, time, lap, lick_count, sensor_counts, msg_buffer);
    }

    public void trialStart(JSONObject[] msg_buffer) {
        this.context_list.trialStart(msg_buffer);
    }

    public void reset() {
        this.context_list.reset();
    }

    public void end() {
        this.context_list.end();
    }

    public boolean isActive() {
        return this.context_list.isActive();
    }

    public int activeIdx() {
        return this.context_list.activeIdx();
    }

    public void suspend() {
        this.context_list.suspend();
    }

    public void stop(float time, JSONObject[] msg_buffer) {
        this.context_list.stop(time, msg_buffer);
    }

    public void shutdown() {
        this.context_list.shutdown();
    }

    public void sendMessage(String message) {
        this.context_list.sendMessage(message);
    }

    public ContextList getContextListBase() {
        return this.context_list;
    }
}

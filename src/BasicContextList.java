import processing.core.PApplet;
import processing.data.JSONObject;
import processing.data.JSONArray;
import java.util.ArrayList;
import java.util.HashMap;
import java.awt.Rectangle;
import java.awt.Point;

/**
 * Controls activating and stopping contexts as the animal progresses along the track.
 * See {@link #check(float, float, int, JSONObject[])} for how this logic is controlled.
 */

//TODO: This doesn't need to extend PApplet (and probably shouldn't)
public class BasicContextList extends PApplet implements ContextList {
//public class BasicContextList implements ContextList {
    /**
     * <code>ArrayList</code> of <code>Context</code> objects holding information regarding the
     * time and location different contexts should be active. Contexts become inactive after
     * they're triggered and resent with each lap. Only 1 <code>Context</code> in this
     * <code>ArrayList</code> may be active at any given moment.
     */
    protected ArrayList<Context> contexts;

    /**
     * Distance (in mm) around each location that context spans.
     */
    protected Point size;

    /**
     * ? - Used when displaying
     */
    protected float scale;

    /**
     * Amount of time (in seconds) that a context may remain active once it has been triggered.
     */
    protected float duration;

    /**
     * Integer corresponding to the index of the currently active context in the
     * <code>ArrayList</code> of contexts. If -1 then no context is currently active.
     */
    protected int active;

    /**
     * Stores the time the last update was sent to this context in seconds.
     */
    protected float sent;

    /**
     * If true, a message has been sent and the ContextList is waiting for a response.
     */
    protected boolean waiting;

    /**
     * Counts the number of tries to send a message to the arduino so the
     * context can send reset messages if nothing is getting through.
     * Todo: is this the number of times to resend message before resetting messages?
     */
    protected int tries;

    /**
     * A status string to be displayed in the UI.
     */
    protected String status;

    /**
     * The length of each lap (in mm).
     */
    protected float track_length;

    /**
     * The color to represent the current context in the UI. Stored as an array of 3 ints,
     * representing red, green, and blue pixels.
     */
    protected int[] display_color;

    /**
     * The radius to represent this context as in the UI.
     */
    protected Point display_size;

    /**
     * UdpClient for sending messages to which relate to this context.
     * Todo: possible better description: Used to send messages to the arduino.
     */
    protected UdpClient comm;

    /**
     * ?
     */
    protected String comm_id;

    /**
     * An identifier for use with the UI and the behavior file.
     */
    protected String id;

    /**
     * UDP message to be sent at the start of each <code>Context</code> in the
     * <code>ContextList</code>
     */
    protected String startString;

    /**
     * UDP message to be sent at the end of each <code>Context</code> in the
     * <code>ContextList</code>
     */
    protected String stopString;

    /**
     * Contains configuration information for this instance's <code>ContextList</code>.
     */
    protected JSONObject context_info;

    /**
     * ?
     */
    protected Boolean fixed_duration;

    /**
     * ?
     */
    protected JSONObject log_json;

    /**
     * Constructor.
     *
     * @param context_info Contains configuration information for this context from the
     *                     settings file.
     * @param track_length The length of the track (in mm).
     * @param comm_id      ?
     *
     */
    public BasicContextList(JSONObject context_info, float track_length,
                            String comm_id) {
        this.contexts = new ArrayList<Context>();
        this.comm = null;
        this.comm_id = comm_id;
        this.sent = -1;
        this.tries = 0;
        this.waiting = false;
        this.display_size = new Point(1, 1);
        this.fixed_duration = context_info.getBoolean("fixed_duration", false);

        this.log_json = new JSONObject();
        this.log_json.setJSONObject("context", new JSONObject());
        if (!context_info.isNull("class")) {
            this.log_json.getJSONObject("context")
                         .setString("class", context_info.getString("class"));
        }

        // sets startString and stopString as well as the id field
        setId(context_info.getString("id"));

        this.duration = context_info.getFloat("max_duration", -1);
        if (context_info.isNull("size")) {
            System.out.println(context_info.toString());
        }
        JSONArray size_array = context_info.getJSONArray("size");
        this.size = new Point(size_array.getInt(0), size_array.getInt(1));
        this.active = -1;
        this.status = "";
        this.track_length = track_length;
        this.context_info = context_info;

        // resolve the display color from rbg in the settings to an integer
        if (!context_info.isNull("display_color")) {
            JSONArray disp_color = context_info.getJSONArray("display_color");
            this.display_color = new int[] {disp_color.getInt(0),
                disp_color.getInt(1), disp_color.getInt(2)};
        } else {
            display_color = null;
        }

        // positions the contexts
        JSONArray locations = null;
        locations = context_info.getJSONArray("locations");

        // if locations is null - specific locations for this context are not
        // supplied
        if (locations != null) {
            for (int i=0; i < locations.size(); i++) {
                add(locations.getJSONArray(i));
            }
        }
    }

    /**
     *
     * @return The <code>UdpClient</code> object belonging to this instance.
     */
    public UdpClient getComm() {
        return this.comm;
    }

    /**
     * ?
     */
    public void sendCreateMessages() {
        // comm may be null for certain subclasses of ContextList which to not
        // need to talk to the behavior arduino
        if (comm != null) {
            context_info.setString("action", "create");
            JSONObject context_setup_json = new JSONObject();
            context_setup_json.setJSONObject("contexts", context_info);

            // configure the valves, the pins which have devices responsible for
            // controlling this context
            JSONArray valves = null;
            if (!context_info.isNull("valves")) {
                valves = context_info.getJSONArray("valves");
            }

            for (int i=0; ((valves != null) && (i < valves.size())); i++) {
                int valve_pin = valves.getInt(i);
                JSONObject valve_json;

                // frequency causes this singal to oscillate in order to play a
                // tone
                if (!context_info.isNull("frequency")) {
                    valve_json = TreadmillController.setup_valve_json(valve_pin,
                        context_info.getInt("frequency"));
                } else if (!context_info.isNull("inverted")) {
                    valve_json = TreadmillController.setup_valve_json(
                        valve_pin, context_info.getBoolean("inverted"));
                } else {
                    valve_json = TreadmillController.setup_valve_json(
                        valve_pin);
                }
                comm.sendMessage(valve_json.toString());
                JSONObject close_json = TreadmillController.close_valve_json(
                    valve_pin);
                comm.sendMessage(close_json.toString());
            }

            this.active = -1;
            this.status = "reset";
            this.tries = 0;
            this.waiting = false;
            comm.sendMessage(context_setup_json.toString());
        } else {
            System.out.println(
                "[" +this.id+ " "  + this.comm_id + "] SEND CREATE MESSAGES FAILED");
        }
    }

    /**
     * Setter method for this BasicContextList's UdpClient.
     *
     * @param comms channel to post messages for configuring, starting or stopping contexts.
     * @return <code>true</code> if the messages were successfully sent, <code>false</code> otherwise.
     */
    public boolean setupComms(ArrayList<UdpClient> comms) {
        for (UdpClient c: comms) {
            if (c.getId().equals(this.comm_id)) {
                this.comm = c;
                break;
            }
        }

        if (this.comm == null) {
            System.out.println("[" + this.id + " "  + this.comm_id + "] FAILED TO FIND COMM");
            return false;
        }

        sendCreateMessages();
        return true;
    }

    /**
     * ?
     *
     * @param contexts ?
     */
    public void registerContexts(ArrayList<ContextList> contexts) { }

    /**
     * Setter method for the id of this BasicContextList.
     * Also configures the startString and stopString valves.
     *
     * @param id Sent to this BasicContextList's <code>UdpClient</code>(<code>comm</code>)
     *           to identify this <code>BasicContextList</code>
     */
    protected void setId(String id) {
        this.id = id;
        this.log_json.getJSONObject("context").setString("id", id);

        JSONObject context_message = new JSONObject();
        context_message.setString("action", "start");
        context_message.setString("id", this.id);
        JSONObject context_message_json = new JSONObject();
        context_message_json.setJSONObject("contexts", context_message);
        this.startString = context_message_json.toString();

        context_message.setString("action", "stop");
        context_message_json.setJSONObject("contexts", context_message);
        this.stopString = context_message_json.toString();
    }

    /**
     *
     * @return ?
     */
    public String getCommId() {
        return this.comm_id;
    }

    /**
     * Returns the id of this BasicContextList.
     *
     * @return the identifier
     */
    public String getId() {
        return this.id;
    }

    /**
     * Sets the length, in mm, the contexts will span in either direction.
     * @param radius
     */
    public void setSize(Point size) {
        if (size != null) {

            for (Context context : this.contexts) {
                context.setSize(size);
            }
        }

        this.size = size;
        this.setDisplayScale(this.scale);
    }

    /**
     *
     * @return An int representing the length, in mm, the contexts span in either direction.
     */
    public Point getSize() {
        return this.size;
    }

    /**
     *
     * @return The length of the track in mm.
     */
    public float getTrackLength() {
        return this.track_length;
    }

    /**
     * Sets the scaling used for displaying this BasicContextList's radius in the UI.
     *
     * @param scale the amount to scale the radius so it displays properly in the UI.
     *              Units are in pixel/mm.
     */
    public void setDisplayScale(float scale) {
        this.scale = scale;
        this.display_size.move(
            (int) (this.size.x * scale), (int) (this.size.y * scale));
    }

    /**
     *
     * @return the scaled width, in pixels, used to draw this BasicContextList's radius in the UI.
     */
    public Point displaySize() {
        return this.display_size;
    }

    /**
     *
     * @return An array of 3 integers, representing the red, green, and blue pixels (in the order)
     *         used to display the currently active context.
     */
    public int[] displayColor() {
        return this.display_color;
    }

    /**
     * Sets the string displayed in the UI describing the state of the contexts in this
     * BasicContextList.
     *
     * @param status The status to display in the UI.
     */
    public void setStatus(String status) {
        this.status = status;

        // if the status has been updated, then the last update has reached the
        // arduino
        waiting = false;
        this.tries = 0;
    }

    /**
     *
     * @return the string representing the current status of the contexts in the list.
     */
    public String getStatus() {
        return this.status;
    }

    /**
     *
     * @return The number of contexts wrapped by this BasicContextList.
     */
    public int size() {
        return this.contexts.size();
    }

    /**
     * Accessor for the location of a specific Context in the List.
     *
     * @param i index of the context to return
     * @return  The location of the context at the supplied index.
     */
    public Point getLocation(int i) {
        return this.contexts.get(i).location();
    }

    /**
     * Accessor for a specific Context in the List.
     *
     * @param i index of the context to return
     * @return  The context at the supplied index.
     */
    public Context getContext(int i) {
        return this.contexts.get(i);
    }

    /**
     * Add a new context to this BasicContext list at the given location.
     *
     * @param location Distance, in mm, from the start of the track to place this context.
     */
    protected void add(JSONArray location) {
        Point pt = new Point(location.getInt(0), location.getInt(1));
        this.contexts.add(new Context(
                pt, this.duration, this.size, this.contexts.size(),
                this.fixed_duration));
    }

    protected void add(Point pt) {
        this.contexts.add(new Context(
                pt, this.duration, this.size, this.contexts.size(),
                this.fixed_duration));
    }

    /**
     * Moves the context at the given index in <code>contexts</code> to the provided location (in mm).
     *
     * @param index The index of the context in <code>contexts</code>
     * @param location The new location of the context, in mm.
     */
    public void move(int index, Point location) {
        this.contexts.get(index).move(location);
    }

    /**
     * Removes all contexts from this BasicContextList.
     */
    public void clear() {
//        if (this.size() > 0) {
//            this.contexts = new ArrayList<Context>();
//        }
        // Is there a reason to check if size() > 0?
        contexts = new ArrayList<Context>();
    }

    /**
     * ?
     *
     * @param msg_buffer ?
     */
    public void trialStart(JSONObject[] msg_buffer) { }

    /**
     * Resets the state of the contexts. Contexts which have been triggered are
     * reactivated and allowed to be triggered again. If <code>shuffle_contexts</code>
     * is <code>true</code>, the contexts will be shuffled.
     */
    public void reset() {
        for (Context context : contexts) {
            context.reset();
        }
    }

    /**
     * Resets the state of the contexts. Contexts which have been triggered are
     * reactivated and allowed to be triggered again. If <code>shuffle_contexts</code>
     * is <code>true</code>, the contexts will be shuffled.
     */
    public void end() {
        this.reset();
    }

    /**
     * An array whose ith element contains the location of the ith context of this
     * BasicContextList.
     *
     * @return An array containing context locations.
     */
    public int[][] toList() {
        int[][] list = new int[contexts.size()][4];
        for (int i = 0; i < contexts.size(); i++) {
            Rectangle location = contexts.get(i).location;
            list[i] = new int[]{location.x, location.y, location.width, location.height};
        }

        return list;
    }

    /**
     * Check the state of the contexts contained in this list and send the
     * start/stop messages as necessary. This method gets called for each cycle
     * of the event loop when a trial is started. Written as a helper method to
     * call <code>check()</code> without <tt>lick_count</tt>. Supports creating subclasses of
     * ContextList with logic based on <tt>lick_count</tt> added.
     *
     * @param position   Current position on the track (in mm).
     * @param time       Time (in seconds) since the start of the trial.
     * @param lap        Current lap number since the start of the trial.
     * @param lick_count Current number of licks, this trial.
     * @param msg_buffer Array to buffer and send messages
     *                   to be logged in the .tdml file being written for
     *                   this trial. Messages should be placed in index 0 of the
     *                   message buffer and must be JSON formatted strings.
     * @return           <code>true</code> to indicate that the trial has started.
     *                   Note: all messages to the behavior comm are sent from
     *                   within this method returning true or false indicates
     *                   the state of the context, but does not actually
     *                   influence the connected arduinos or UI.
     */
    protected boolean check(Point position, float time, int lap, int lick_count,
                         JSONObject[] msg_buffer) {

        return check(position, time, lap, msg_buffer);
    }

    /**
     * Check the state of the contexts contained in this list and send the
     * start/stop messages as necessary. This method gets called for each cycle
     * of the event loop when a trial is started. Written as a helper method to
     * call check without <tt>lick_count</tt> or <tt>sensor_counts</tt>.
     * Supports creating subclasses of ContextList with logic based on
     * <tt>lick_count</tt> and <tt>sensor_counts</tt> added.S
     *
     * @param position   Current position on the track (in mm).
     * @param time       Time (in seconds) since the start of the trial.
     * @param lap        Current lap number since the start of the trial.
     * @param lick_count Current number of licks, this trial.
     * @param sensor_counts ?
     * @param msg_buffer Array to buffer and send messages
     *                   to be logged in the .tdml file being written for
     *                   this trial. Messages should be placed in index 0 of the
     *                   message buffer and must be JSON formatted strings.
     * @return           <code>true</code> to indicate that the trial has started.
     *                   Note: all messages to the behavior comm are sent from
     *                   within this method returning true or false indicates
     *                   the state of the context, but does not actually
     *                   influence the connected arduinos or UI.
     */
    public boolean check(Point position, float time, int lap, int lick_count, HashMap<Integer,
            Integer> sensor_counts, JSONObject[] msg_buffer) {

        return check(position, time, lap, msg_buffer);
    }

    /**
     * Check the state of the contexts contained in this list and send the
     * start/stop messages as necessary. This method gets called for each cycle
     * of the event loop when a trial is started.
     *
     * @param position   current position along the track
     * @param time       time (in s) since the start of the trial
     * @param lap        current lap number since the start of the trial
     * @param msg_buffer a Java array of type String to buffer and send messages
     *                   to be logged in the the tdml file being written for
     *                   this trial. messages should be placed in index 0 of the
     *                   message buffer and must be JSON formatted strings.
     * @return           <code>true</code> to indicate that the trial has started.
     *                   Note: all messages to the behavior comm are sent from
     *                   within this method returning true or false indicates
     *                   the state of the context, but does not actually
     *                   influence the connected arduinos or UI.
     */
    protected boolean check(Point position, float time, int lap, JSONObject[] msg_buffer) {
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

    /**
     *
     * @return <code>true</code> if there is currently an active context or <code>false</code>
     * if all contexts are suspended.
     */
    public boolean isActive() {
        return this.active != -1;
    }

    /**
     *
     * @return The index of the currently active context.
     */
    public int activeIdx() {
        return this.active;
    }

    // Todo: suspend should probably be removed from BasicContextList
    /**
     * Suspend all contexts and send a "send stop" message.
     */
    public void suspend() {
        this.active = -1;
        this.status = "sent stop";
        this.sendMessage(this.stopString);
    }

    /**
     * Todo: seems to do the same thing as suspend. What are the parameters for?
     * Stop this context. Called at the end of trials to ensure that the context is shut off.
     *
     * @param time ?
     * @param msg_buffer ?
     */
    public void stop(float time, JSONObject[] msg_buffer) {
        this.active = -1;
        this.status = "sent stop";
        this.waiting = false;
        this.sendMessage(this.stopString);
    }
    // suspend vs stop: stop means the mouse "completed" or "ran through" the context whereas
    // suspend means it shouldn't be active for this lap
    /**
     * Todo: does this send a message to the arduino?
     *
     * @param message ?
     */
    public void sendMessage(String message) {
        this.comm.sendMessage(message);
    }

    /**
     * ?
     */
    public void shutdown() { }
}

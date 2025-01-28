import java.util.HashMap;
import processing.data.JSONObject;
import java.awt.Point;

public class DepletingContextList extends BasicContextList {

    /**
     * Store whether the context is currently active for this lap, or suspended.
     */
    private boolean suspended;

    protected float t_start;
    protected float t;
    protected float p_reward;
    protected float a;
    protected float C;
    protected float b0;
    protected float b1;
    protected float o;

    protected int sensor;
    protected int sensor_count;
    protected int reward_count;
    protected boolean count_stops;

    protected TreadmillController tc;
    /**
     * The color of the context on the display when it is suspended.
     */
    protected int[] display_color_suspended;

    public DepletingContextList(JSONObject context_info, float track_length, String comm_id, int sensor, TreadmillController tc) {
        super(context_info, track_length, comm_id);

        this.suspended = false;
        this.t_start = -1;
        this.t = -1;
        this.p_reward = -1f;
        this.sensor = sensor;
        this.reward_count = 0;

        this.count_stops = context_info.getBoolean("count_stops", false);
        this.tc = tc;

        JSONObject rate_params = context_info.getJSONObject("rate_params");
        this.a = rate_params.getFloat("a", 0.8f);
        this.C = rate_params.getFloat("C", -2.6f);
        this.b0 = rate_params.getFloat("b0", 0.3f);
        this.b1 = rate_params.getFloat("b1", -0.03f);
        this.o = rate_params.getFloat("o", 0.0f);
    }

    public void stop(float time, JSONObject[] msg_buffer) {
        if (this.count_stops) {
            this.tc.increment_trial();
        }
        super.stop(time, msg_buffer);
    }

    /**
     * @return An array of 3 integers, representing the red, green, and blue pixels (in the order)
     *         used to display the wrapped ContextList's currently active context.
     */
    //public int[] displayColor() {
    //    return this.displayColor();
    //}

    /**
     *
     * @return The string representing the current status of the contexts.
     */
    //public String getStatus() {
    //    if this.status()
    //    return Float.toString(this.p_reward);
    //}


    /**
     * Check the state of the list as well as the contexts contained in this
     * and decide if they should be activated or not. Send the start/stop messages
     * as necessary. this method gets called for each cycle of the event loop
     * when a trial is started.
     *
     * @param position current position along the track
     * @param time time (in seconds) since the start of the trial
     * @param lap current lap number since the start of the trial
     * @param lick_count ?
     * @param sensor_counts ?
     * @param msg_buffer a Java array of type String to buffer and send messages
     *                   to be logged in the .tdml file being written for
     *                   this trial. Messages should be placed in index 0 of the
     *                   message buffer and must be JSON-formatted strings.
     * @return           ?
     */
    public boolean check(Point position, float time, int lap, int lick_count,
                         HashMap<Integer, Integer> sensor_counts, JSONObject[] msg_buffer) {

        // If the context list is not suspended call the check method for the default ContextList
        // behavior.
        int active_idx = this.activeIdx();
        boolean activate = super.check(position, time, lap, lick_count,
                                       sensor_counts, msg_buffer);
        if (activate && (t_start == -1)) {
            this.t_start = time;
            this.sendMessage(this.startString);
            this.sensor_count = sensor_counts.get(this.sensor);
            this.reward_count++;
            return true;
        }

        if (activate) {
            this.t = time - this.t_start;
            this.p_reward = this.a * (float)Math.exp(this.C * (this.t - this.o)) + this.b0 + this.b1 * this.t;
            this.status = Integer.toString(this.reward_count);

            float rand_num = (float) Math.random();
            if ((this.sensor_count != sensor_counts.get(this.sensor)) &&
                (rand_num < this.p_reward)) {

                this.sensor_count = sensor_counts.get(this.sensor);
                this.sendMessage(this.startString);
                this.reward_count++;
                return true;
            }
        } else if ((t_start != -1) && (!activate)) {
            this.t_start = -1;
            this.p_reward = -1;
            this.reward_count = 0;
            this.getContext(active_idx).disable();
            if (this.count_stops) {
                this.tc.increment_trial();
            }
        }

        if ((this.p_reward < 0) && (activate)) {
            this.t_start = -1;
            this.p_reward = -1;
            this.reward_count = 0;
            this.stop(time, msg_buffer);
            this.getContext(active_idx).disable();
        }

        this.sensor_count = sensor_counts.get(this.sensor);
        return false;
    }
}

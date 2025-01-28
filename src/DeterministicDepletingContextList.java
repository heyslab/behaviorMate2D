import java.util.HashMap;
import processing.data.JSONObject;
import java.awt.Point;

public class DeterministicDepletingContextList extends BasicContextList {

    protected float a;
    protected float C;
    protected float b0;
    protected float b1;
    protected float o;

    protected int reward_count;
    protected boolean count_stops;
    protected float next_reward;
    protected float lick_window;
    protected int sensor;
    protected int sensor_count;
    protected float last_lick;

    protected TreadmillController tc;
    /**
     * The color of the context on the display when it is suspended.
     */
    protected int[] display_color_suspended;

    public DeterministicDepletingContextList(JSONObject context_info,
                                             float track_length,
                                             String comm_id,
                                             TreadmillController tc) {
        super(context_info, track_length, comm_id);

        this.next_reward = -1f;
        this.reward_count = 0;
        this.last_lick = 0;
        this.sensor_count = 0;

        this.count_stops = context_info.getBoolean("count_stops", false);
        this.tc = tc;

        if (!context_info.isNull("lick_window")) {
            this.lick_window = context_info.getFloat("lick_window");
            this.sensor = context_info.getInt("sensor");
        } else {
            this.sensor = -1;
            this.lick_window = -1;
        }

        JSONObject rate_params;
        if (context_info.isNull("rate_params")) {
            rate_params = new JSONObject();
        } else {
            rate_params = context_info.getJSONObject("rate_params");
        }
        this.a = rate_params.getFloat("a", 3.8f);
        this.C = rate_params.getFloat("C", 0.1f);
        this.b0 = rate_params.getFloat("b0", 5.0f);
        this.b1 = rate_params.getFloat("b1", 0.0f);
        this.o = rate_params.getFloat("o", 2.0f);
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


    public float calculate_next_reward(float t) {
        return this.b0 - this.a * (float)Math.exp(-(t - this.o) * this.C);
    }

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
                         HashMap<Integer, Integer> sensor_counts,
                         JSONObject[] msg_buffer) {

        int pre_active_idx = this.activeIdx();
        boolean activate = super.check(position, time, lap, lick_count,
                                       sensor_counts, msg_buffer);
        int active_idx = this.activeIdx();
        if (pre_active_idx != active_idx) {
            if (active_idx == -1) { // Turn off
                this.reward_count = 0;
                if (this.count_stops) {
                    this.tc.increment_trial();
                }

                return false;
            } else {
                this.next_reward = time + calculate_next_reward(
                    time - this.getContext(active_idx).started_time);
            }
        }

        if (active_idx != -1) {
            if (this.lick_window != -1) {
                if (sensor_counts.get(this.sensor) != this.sensor_count) {
                    this.last_lick = time;
                    this.sensor_count = sensor_counts.get(this.sensor);
                } else if ((time - this.last_lick) > this.lick_window) {
                    this.getContext(active_idx).disable();
                    return false;
                }
            }

            this.status = Integer.toString(this.reward_count);
            if (time > this.next_reward) {
                this.sendMessage(this.startString);
                this.reward_count++;

                this.next_reward = time + calculate_next_reward(
                    time - this.getContext(active_idx).started_time);
                return true;
            }
        }

        return false;
    }
}

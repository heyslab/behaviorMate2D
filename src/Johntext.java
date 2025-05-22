import java.util.HashMap;
import processing.data.JSONObject;
import java.awt.Point;
import java.util.concurrent.ThreadLocalRandom;

public class Johntext extends BasicContextList {

    /**
     * Store whether the context is currently active for this lap, or suspended.
     */
    private boolean suspended;

    protected float t_start;
    protected float t_lick_start;
    protected float t;
    protected float rate_func;
    protected float tauS;
    protected float tauD;
    protected float r0;
    protected float V0;
    protected float Vr;
    protected float event_time;
    protected float c_vol;
    protected float lambda0;
    protected String speaker_start;
    protected int speaker_pin;
    protected int speaker_freq;
    protected String reward_start;
    protected int reward_pin;
    protected int reward_duration;
    protected float lick_window;
    protected float t_last_lick;
    // protected float o;

    protected int sensor;
    protected int sensor_count;
    protected int reward_count;
    protected boolean count_stops;

    protected TreadmillController tc;
    /**
     * The color of the context on the display when it is suspended.
     */
    protected int[] display_color_suspended;

    public Johntext(JSONObject context_info, float track_length, String comm_id, int sensor, TreadmillController tc) {
        super(context_info, track_length, comm_id);

        this.suspended = false;
        this.t_start = -1;
        this.t_lick_start = -1;
        this.t = -1;
        // this.p_reward = -1f;
        this.sensor = sensor;
        this.reward_count = 0;
        this.event_time = -1;

        this.count_stops = context_info.getBoolean("count_stops", false);
        this.tc = tc;
        this.t_last_lick = 0;

        JSONObject rate_params = context_info.getJSONObject("rate_params");
        this.tauD = rate_params.getJSONArray("tau").getFloat(0);
        this.tauS = rate_params.getJSONArray("tau").getFloat(1);
        
        this.r0 = rate_params.getFloat("r0", 2.5f);
        this.V0 = rate_params.getFloat("V0", 0.1f);
        this.Vr = rate_params.getFloat("Vr", 2f);
        // this.o = rate_params.getFloat("o", 0.0f);
        this.lambda0 = this.r0 / this.V0;
        this.c_vol = 0;

        this.speaker_pin = context_info.getInt("speaker_pin");
        this.reward_pin = context_info.getInt("reward_pin");
        this.reward_duration = context_info.getInt("reward_duration");
        int speaker_duration = context_info.getInt("speaker_duration", 500);
        this.speaker_freq = context_info.getInt("speaker_freq");

        this.speaker_start = tc.open_valve_json(speaker_pin, speaker_duration, speaker_freq).toString();
        this.reward_start = tc.open_valve_json(reward_pin, reward_duration).toString();

        this.lick_window = context_info.getFloat("lick_window");
    }

    public void sendCreateMessages() {
        super.sendCreateMessages();
        JSONObject speaker = tc.setup_valve_json(this.speaker_pin, this.speaker_freq);
        JSONObject reward = tc.setup_valve_json(this.reward_pin);
        this.comm.sendMessage(speaker.toString());
        this.comm.sendMessage(reward.toString());
    }

    public void reset() {
        super.reset();
    }

    public void end() {
        this.t_start = -1;
        this.t_lick_start = -1;
        this.event_time = -1;
        this.c_vol = 0;
        this.reward_count = 0;
        this.reset();
        super.end();
    }

    public void trialStart(JSONObject[] msg_buffer) {
        super.trialStart(msg_buffer);
    }

    public void stop(float time, JSONObject[] msg_buffer) {
        if (this.count_stops) {
            this.tc.increment_trial();
        }
        super.stop(time, msg_buffer);
    }

    public float expRNG(float rate) {
        return - ((float) Math.log(1 - ThreadLocalRandom.current().nextFloat())) / rate;
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

        float tau = this.tauD;

        if ((lap > 30) & (lap <= 60)) {
            tau = this.tauS;
        }
        
        int active_idx = this.activeIdx();
        boolean entered = super.check(position, time, lap, lick_count,
                                       sensor_counts, msg_buffer);

        
        boolean licked = sensor_counts.get(this.sensor) != this.sensor_count;
        System.out.println("lick window? "+((time - this.t_last_lick) > this.lick_window));

        if (entered) {

            if (t_start == -1) {
                this.sendMessage(this.speaker_start);
                this.t_start = time;
            }
            if (licked) {
                this.t_last_lick = time;
            }

            // if ((time - t_last_lick) > this.lick_window) {
            //     return false;
            // }

            if ((licked) && (t_lick_start == -1)){

                this.t_lick_start = time;
                this.sendMessage(this.reward_start);
                this.sensor_count = sensor_counts.get(this.sensor);
                this.reward_count++;
                return true;
            }
            
            if (this.t_lick_start == -1) {
                return false; // Don't compute rewards until first lick
            }

            this.t = time - this.t_lick_start;
            this.rate_func = this.lambda0 * (float) Math.exp(-this.t / tau);
            this.status = Integer.toString(this.reward_count);
            
            if (this.event_time == -1) {
                float dt = (float) expRNG(this.rate_func);
                this.event_time = this.t + dt;
            }
            else if (this.t > this.event_time) {
                float dt = (float) expRNG(this.rate_func);
                this.c_vol = this.c_vol + this.V0;
                this.event_time = this.t + dt;
            }
            
            if (licked && (this.c_vol >= this.Vr) && ((time - t_last_lick) < this.lick_window)) {
                this.c_vol = 0;
                this.sendMessage(this.reward_start);
                this.reward_count++;
            } 

            this.sensor_count = sensor_counts.get(this.sensor);
            return true;

        } else if ((t_lick_start != -1) && (!entered)) {
            this.t_start = -1;
            this.t_lick_start = -1;
            this.event_time = -1;
            this.c_vol = 0;
            this.reward_count = 0;
            this.getContext(active_idx).disable();
            if (this.count_stops) {
                this.tc.increment_trial();
            }
        }

        this.sensor_count = sensor_counts.get(this.sensor);
        return false;
        
    }
}

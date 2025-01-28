import java.util.HashMap;
import processing.data.JSONObject;
import java.awt.Point;

/**
 * ?
 */
public class LickStartContextDecorator extends ContextListDecorator {

    /**
     * ?
     */
    private int prev_lickcount;

    /**
     * ?
     */
    protected int last_position;

    /**
     * ?
     */
    protected float entered_time;

    /**
     * ?
     */
    protected float max_time;

    /**
     * ?
     */
    protected int timeInPosition;

    protected int pin;

    /**
     * ?
     *
     * @param context_list <code>ContextList</code> instance the decorator will wrap.
     * @param context_info JSONObject containing the configuration information for this context
     *                     from the settings file. The <tt>max_time</tt> parameter is optional
     *                     and will default to -1 if not provided.
     */
    public LickStartContextDecorator(ContextList context_list,
                                     JSONObject context_info, int pin) {
        super(context_list);
        this.last_position = -1;
        this.entered_time = -1;
        this.prev_lickcount = -1;

        this.max_time = context_info.getInt("max_time", -1);
        this.pin = pin;
    }

    /**
     * Resets the decorator's attributes to constructor defaults and resets the wrapped
     * <code>ContextList</code>.
     */
    public void reset() {
        this.last_position = -1;
        this.entered_time = -1;
        super.reset();
    }

    public void end() {
        this.prev_lickcount = -1;
        this.reset();
        super.end();
    }

    public void trialStart(JSONObject[] msg_buffer) {
        this.prev_lickcount = -1;
        super.trialStart(msg_buffer);
    }

    /**
     * Check the state of the list as well as the contexts contained in this and decide if they
     * should be activated or not. Send the start/stop messages as necessary. this method gets
     * called for each cycle of the event loop when a trial is started.
     *
     * @param position   Current position along the track in millimeters.
     * @param time       Time (in s) since the start of the trial.
     * @param lap        Current lap number since the start of the trial.
     * @param lick_count ?
     * @param msg_buffer A Java <code>String</code> array of type to buffer and send messages to be
     *                   logged in the .tdml file being written for this trial. messages should
     *                   be placed at index 0 of the message buffer and must be JSON-formatted strings.
     * @return           ?
     */
    public boolean check(Point position, float time, int lap, int lick_count,
                         HashMap<Integer, Integer> sensor_counts, JSONObject[] msg_buffer) {

        //boolean inPosition = (this.max_time == -1);
        if (this.prev_lickcount == -1) {
            this.prev_lickcount = sensor_counts.get(this.pin);
            return false;
        }

        boolean inPosition = false;
        if (!this.context_list.isActive()) {
            for (int i = 0; (i < this.context_list.size()); i++) {
                if (this.context_list.getContext(i).checkPosition(position)) {
                    inPosition = true;
                    if ((this.entered_time == -1) || (i != this.last_position)){
                        this.context_list.setStatus("no lick");
                        this.entered_time = time;
                        this.last_position = i;
                        break;
                    } else if ((i == this.last_position) && (max_time != -1)) {
                        if (this.entered_time + this.max_time < time) {
                            this.prev_lickcount = sensor_counts.get(this.pin);
                            this.context_list.setStatus("timed out");
                            return false;
                        }
                    }
                }
            }

            if (!inPosition) {
                this.entered_time = -1;
                this.context_list.setStatus("stopped");

                this.prev_lickcount = sensor_counts.get(this.pin);
                return false;
            }

            if (sensor_counts.get(this.pin) != prev_lickcount) {
                this.prev_lickcount = sensor_counts.get(this.pin);
                return super.check(
                        position, time, lap, lick_count, sensor_counts, msg_buffer);
            }

            this.prev_lickcount = sensor_counts.get(this.pin);
            return false;
        }

        this.prev_lickcount = sensor_counts.get(this.pin);
        return super.check(
                position, time, lap, lick_count, sensor_counts,  msg_buffer);
    }
}

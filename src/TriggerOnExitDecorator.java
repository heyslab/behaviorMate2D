import processing.data.JSONObject;
import java.util.ArrayList;
import java.awt.Point;
import java.util.HashMap;

/**
 * ?
 */
public class TriggerOnExitDecorator extends ContextListDecorator {

    protected String joint_id;

    protected ContextList joint_list;

    protected boolean joint_active;

    /**
     * ?
     *
     * @param context_list <code>ContextList</code> instance the decorator will wrap.
     * @param context_info JSONObject containing the configuration information for this context
     *                     from the settings file. <tt>context_info</tt> should have the parameter
     *                     <tt>joint_id</tt> set to do ?. The <tt>master</tt> parameter is optional
     *                     and will default to false if not provided.
     */
    public TriggerOnExitDecorator(ContextList context_list,
                                  JSONObject context_info) {
        super(context_list);

        this.joint_id = context_info.getString("joint_id");
        this.joint_active = false;
        this.joint_list = null;
    }

    /**
     * ?
     *
     * @param contexts ?
     */
    public void registerContexts(ArrayList<ContextList> contexts) {
        for (int i = 0; i < contexts.size(); i++) {
            ContextList context_list = contexts.get(i);
            if (context_list.getId().equals(this.joint_id)) {
                this.joint_list = context_list;

                break;
            }
        }
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
        boolean joint_update = this.joint_list.isActive();

        if (this.joint_active && !joint_update) {
            this.joint_active = false;
            this.context_list.reset();
        } else if (!this.joint_active && joint_update) {
            this.joint_active = true;
        }

        return super.check(position, time, lap, lick_count, sensor_counts, msg_buffer);
    }
}

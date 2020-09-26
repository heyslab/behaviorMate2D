import processing.data.JSONObject;
import processing.data.JSONArray;
import java.util.ArrayList;

public class VrFogContext extends VrContext {
    protected ArrayList<Integer> fog_starts;
    protected ArrayList<Integer> fog_ends;
    protected ArrayList<Integer> fog_locations;

    protected int fog_start;
    protected int fog_end;
    protected int fog_location_idx;

    protected JSONObject fog_message;

    public VrFogContext(
            TreadmillController tc, JSONObject context_info,
            float track_length) throws Exception {

        super(tc, context_info, track_length);

        JSONArray fog_starts_j = this.context_info.getJSONArray("fog_starts");
        this.fog_starts = new ArrayList<Integer>();
        for (int i = 0; i < fog_starts_j.size(); i++) {
            fog_starts.add(fog_starts_j.getInt(i));
        }

        JSONArray fog_ends_j = this.context_info.getJSONArray("fog_ends");
        this.fog_ends = new ArrayList<Integer>();
        for (int i = 0; i < fog_ends_j.size(); i++) {
            fog_ends.add(fog_ends_j.getInt(i));
        }

        JSONArray fog_locations_j = this.context_info.getJSONArray("fog_locations");
        this.fog_locations = new ArrayList<Integer>();
        for (int i = 0; i < fog_locations_j.size(); i++) {
            fog_locations.add(fog_locations_j.getInt(i));
        }

        this.fog_message = new JSONObject();
        this.fog_message.setJSONObject("fog", new JSONObject());
        this.fog_message.getJSONObject("fog").setInt(
            "start", this.fog_starts.get(0));
        this.fog_message.getJSONObject("fog").setInt(
            "end", this.fog_ends.get(0));
        this.fog_start = -1;
        this.fog_end = -1;

        JSONObject fog_json = new JSONObject();
        fog_json.setJSONObject("fog", new JSONObject());
        fog_json.getJSONObject("fog").setString("action", "start");
        this.startString = fog_json.toString();
        fog_json.getJSONObject("fog").setString("action", "stop");
        this.stopString = fog_json.toString();
    }


    protected void updateVr(boolean is_active, float position, float time,
                            int lap) {
        if (is_active) {
            float x = (
                position - (this.getLocation(this.active) - this.getRadius())
                );

            int location_idx = 0;
            for (; this.fog_locations.get(location_idx) <= x; location_idx++){ }
            location_idx -= 1;
            
            int f1 = this.fog_locations.get(location_idx);
            int f2 = this.fog_locations.get(location_idx + 1);
            float scale = (x - f1)/(f2 - f1);
            int new_fog_start = Math.round(
                (this.fog_starts.get(location_idx) +
                 (float)(this.fog_starts.get(location_idx + 1) -
                         this.fog_starts.get(location_idx)) * scale)
                );
            int new_fog_end = Math.round(
                (this.fog_ends.get(location_idx) +
                 (float)(this.fog_ends.get(location_idx  + 1) -
                         this.fog_ends.get(location_idx)) * scale)
                );

            if (( new_fog_start != this.fog_start) || (new_fog_end != this.fog_end)) {
                this.fog_start = new_fog_start;
                this.fog_end = new_fog_end;
                this.fog_message.getJSONObject("fog").setInt(
                    "start", this.fog_start);
                this.fog_message.getJSONObject("fog").setInt(
                    "end", this.fog_end);
                this.sendMessage(this.fog_message.toString());
            }
        }
    }
}

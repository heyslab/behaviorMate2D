
public abstract ContextListDecorator {
    protected ContextList context_list;

    public void ContextListDecorator(ContextList context_list) {
        this.context_list = context_list;
    }

    public void sendCreateMessages() {
        this.context_list.sendCreateMessages();
    }

    public void setComm(UdpClient comm) {
        this.context_list.setComm(comm);
    }

    public String getId() {
        return this.context_list.getId();
    }

    public void setDisplayScale(float scale) {
        this.context_list.setDisplayScale(scale);
    }

    public float displayRadius() {
        return this.context_list.displayRadius();
    }

    public int displayColor() {
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

    public int getLocation(int i) {
        return this.context_list.getLocation(i);
    }

    public void clear() {
        this.context_list.clear();
    }

    public void shuffle() {
        this.context_list.shuffle();
    }

    public int[] toList() {
        return this.context_list.toList();
    }

    public boolean check(float position, float time, int lap, int lick_count,
                         String[] msg_buffer) {

        return this.context_list.check(position, time, lap, lick_count,
            msg_buffer);
    }

    public boolean check(float position, float time, int lap,
                         String[] msg_buffer) {

        return this.context_list.check(position, time, lap, msg_buffer);
    }

    public void stop(float time, String[] msg_buffer) {
        this.context_list.stop(time, msg_buffer);
    }
}

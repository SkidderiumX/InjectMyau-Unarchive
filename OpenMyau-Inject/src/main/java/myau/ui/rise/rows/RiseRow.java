package myau.ui.rise.rows;

public abstract class RiseRow {
    protected float x;
    protected float y;
    protected float width;

    public void layout(float x, float y, float width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public abstract float getHeight();

    public abstract void render(int mouseX, int mouseY);

    public boolean onClick(int mouseX, int mouseY, int button) {
        return false;
    }

    public void onDrag(int mouseX, int mouseY) {
    }

    public void onRelease() {
    }

    public void onKey(char typed, int key) {
    }

    public boolean isBinding() {
        return false;
    }

    public boolean isVisible() {
        return true;
    }

    protected boolean over(int mouseX, int mouseY, float height) {
        return mouseX >= this.x && mouseX <= this.x + this.width
                && mouseY >= this.y && mouseY <= this.y + height;
    }
}

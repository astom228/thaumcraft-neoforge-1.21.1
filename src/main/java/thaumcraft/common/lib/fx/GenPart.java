package thaumcraft.common.lib.fx;

/** Параметры обобщённой частицы (порт FXDispatcher.GenPart): кадры, цвет, прозрачность и размер по времени, слой. */
public class GenPart {
    public int grid = 64;
    public int age = 0;
    public float redStart = 1.0F;
    public float greenStart = 1.0F;
    public float blueStart = 1.0F;
    public float redEnd = 1.0F;
    public float greenEnd = 1.0F;
    public float blueEnd = 1.0F;
    public float[] alpha = {1.0F};
    public float[] scale = {1.0F};
    public float rot;
    public float rotstart = 0.0F;
    public boolean loop = false;
    public int partStart = 0;
    public int partNum = 1;
    public int partInc = 1;
    public int layer = 0;
    public double slowDown = 0.98F;
    public float grav = 0.0F;
    public int delay = 0;
}

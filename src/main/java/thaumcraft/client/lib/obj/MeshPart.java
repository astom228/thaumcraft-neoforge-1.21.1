package thaumcraft.client.lib.obj;

import java.util.ArrayList;
import java.util.List;

/** Часть OBJ-модели: имя группы, материал и индексы вершин (по 4 на грань; треугольник дополняется повтором). Порт MeshPart. */
public class MeshPart {
    public String name;
    public String material;
    public List<int[]> indices;
    public int tintIndex = -1;

    public MeshPart() {
        this.indices = new ArrayList<>();
    }

    public MeshPart(MeshPart p, int ti) {
        this.name = p.name;
        this.material = p.material;
        this.indices = p.indices;
        this.tintIndex = ti;
    }

    public void addTriangleFace(int[] a, int[] b, int[] c) {
        this.indices.add(a);
        this.indices.add(b);
        this.indices.add(c);
        this.indices.add(c);
    }

    public void addQuadFace(int[] a, int[] b, int[] c, int[] d) {
        this.indices.add(a);
        this.indices.add(b);
        this.indices.add(c);
        this.indices.add(d);
    }
}

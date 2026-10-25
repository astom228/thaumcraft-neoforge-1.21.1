package thaumcraft.client.lib;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3d;

/**
 * Замена glePolyCone из библиотеки GLE (com.sasmaster.glelwjgl, CoreGLE), которой в 1.12 рисовались потоки эссенции и лучи:
 * трубка переменного радиуса вдоль ломаной. Как в GLE: первая и последняя точки задают только направление концов (трубка идёт
 * от точки 1 до n-2), кольцо из tess сторон, цвет вершин — по точкам, текстура: u — по окружности (0..1), v — по длине, на каждый
 * отрезок +texSlice начиная с start; «верх» кольца — общий вектор up (как в GLE). Стык отрезков — кольцо в плоскости,
 * перпендикулярной средней касательной (в GLE — косой срез по биссектрисе; на плавных потоках разницы не видно, DECISIONS.md).
 * Вершины пишутся четырёхугольниками в формате POSITION_TEX_COLOR.
 */
public final class PolyCone {

    private PolyCone() {
    }

    public static void draw(VertexConsumer vc, Matrix4f m, int tess, double[][] points, float[][] colours, double[] radii, float texSlice, float start) {
        int n = points.length;
        if (n < 4) {
            return;
        }
        // «верх» как в GLE: по первому невырожденному отрезку; вертикальный — (1,1,1), иначе (0,1,0)
        Vector3d up = new Vector3d(0.0, 1.0, 0.0);
        for (int i = 1; i < n; i++) {
            double dx = points[i][0] - points[i - 1][0];
            double dy = points[i][1] - points[i - 1][1];
            double dz = points[i][2] - points[i - 1][2];
            if (dx != 0.0 || dy != 0.0 || dz != 0.0) {
                if (dx == 0.0 && dz == 0.0) {
                    up.set(1.0, 1.0, 1.0).normalize();
                }
                break;
            }
        }
        // кольца в точках 1..n-2
        float[][][] rings = new float[n][][];
        for (int k = 1; k <= n - 2; k++) {
            Vector3d t1 = dir(points[k - 1], points[k]);
            Vector3d t2 = dir(points[k], points[k + 1]);
            Vector3d t = new Vector3d(t1).add(t2);
            if (t.lengthSquared() < 1.0E-12) {
                t.set(t1.lengthSquared() > 0 ? t1 : t2);
            }
            if (t.lengthSquared() < 1.0E-12) {
                t.set(0.0, 0.0, 1.0);
            }
            t.normalize();
            Vector3d x = new Vector3d(up).sub(new Vector3d(t).mul(up.dot(t)));
            if (x.lengthSquared() < 1.0E-12) {
                x.set(t.y, -t.x, 0.0);
                if (x.lengthSquared() < 1.0E-12) {
                    x.set(1.0, 0.0, 0.0);
                }
            }
            x.normalize();
            Vector3d y = new Vector3d(t).cross(x);
            float[][] ring = new float[tess + 1][3];
            for (int j = 0; j <= tess; j++) {
                double a = Math.PI * 2.0 * (j % tess) / tess;
                double c = Math.cos(a) * radii[k];
                double s = Math.sin(a) * radii[k];
                ring[j][0] = (float) (points[k][0] + x.x * c + y.x * s);
                ring[j][1] = (float) (points[k][1] + x.y * c + y.y * s);
                ring[j][2] = (float) (points[k][2] + x.z * c + y.z * s);
            }
            rings[k] = ring;
        }
        float v = start;
        for (int k = 1; k < n - 2; k++) {
            float[][] a = rings[k];
            float[][] b = rings[k + 1];
            float[] ca = colours[k];
            float[] cb = colours[k + 1];
            for (int j = 0; j < tess; j++) {
                float u0 = (float) j / tess;
                float u1 = (float) (j + 1) / tess;
                vc.addVertex(m, a[j][0], a[j][1], a[j][2]).setUv(u0, v).setColor(ca[0], ca[1], ca[2], ca[3]);
                vc.addVertex(m, b[j][0], b[j][1], b[j][2]).setUv(u0, v + texSlice).setColor(cb[0], cb[1], cb[2], cb[3]);
                vc.addVertex(m, b[j + 1][0], b[j + 1][1], b[j + 1][2]).setUv(u1, v + texSlice).setColor(cb[0], cb[1], cb[2], cb[3]);
                vc.addVertex(m, a[j + 1][0], a[j + 1][1], a[j + 1][2]).setUv(u1, v).setColor(ca[0], ca[1], ca[2], ca[3]);
            }
            v += texSlice;
        }
    }

    private static Vector3d dir(double[] from, double[] to) {
        Vector3d d = new Vector3d(to[0] - from[0], to[1] - from[1], to[2] - from[2]);
        return d.lengthSquared() > 1.0E-12 ? d.normalize() : d;
    }
}

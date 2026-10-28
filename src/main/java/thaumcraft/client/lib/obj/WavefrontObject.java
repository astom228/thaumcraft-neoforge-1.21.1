package thaumcraft.client.lib.obj;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;

/**
 * OBJ-модель (порт WavefrontObject / AdvancedModelLoader оригинала, только нужное): вершины, текстурные координаты (v
 * переворачивается: 1 − v), грани по группам ("g"/"o"). Отрисовка — треугольниками в формат POSITION_TEX_COLOR со сдвигом
 * текстурных координат к центру грани на 0.0005 (textureOffset оригинала). Нормали не нужны — освещение не используется.
 */
public class WavefrontObject {
    private final List<Group> groups = new ArrayList<>();

    private record Face(float[][] verts, float[][] uvs) {
    }

    private record Group(String name, List<Face> faces) {
    }

    public static WavefrontObject load(ResourceLocation res) {
        WavefrontObject obj = new WavefrontObject();
        List<float[]> vs = new ArrayList<>();
        List<float[]> vts = new ArrayList<>();
        Group current = null;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                Minecraft.getInstance().getResourceManager().open(res), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim().replaceAll("\\s+", " ");
                if (line.startsWith("v ")) {
                    String[] t = line.split(" ");
                    vs.add(new float[] {Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3])});
                } else if (line.startsWith("vt ")) {
                    String[] t = line.split(" ");
                    vts.add(new float[] {Float.parseFloat(t[1]), 1.0F - Float.parseFloat(t[2])});
                } else if (line.startsWith("g ") || line.startsWith("o ")) {
                    current = new Group(line.substring(2), new ArrayList<>());
                    obj.groups.add(current);
                } else if (line.startsWith("f ")) {
                    if (current == null) {
                        current = new Group("default", new ArrayList<>());
                        obj.groups.add(current);
                    }
                    String[] t = line.substring(2).split(" ");
                    float[][] fv = new float[t.length][];
                    float[][] ft = new float[t.length][];
                    for (int i = 0; i < t.length; i++) {
                        String[] idx = t[i].split("/");
                        fv[i] = vs.get(Integer.parseInt(idx[0]) - 1);
                        ft[i] = idx.length > 1 && !idx[1].isEmpty() ? vts.get(Integer.parseInt(idx[1]) - 1) : null;
                    }
                    current.faces.add(new Face(fv, ft[0] != null ? ft : null));
                }
            }
        } catch (IOException | RuntimeException e) {
            Thaumcraft.log.error("Не удалось загрузить OBJ-модель {}", res, e);
        }
        return obj;
    }

    public void renderAll(VertexConsumer buf, Matrix4f m, float r, float g, float b, float a) {
        for (Group gr : this.groups) {
            renderGroup(gr, buf, m, r, g, b, a);
        }
    }

    public void renderOnly(VertexConsumer buf, Matrix4f m, float r, float g, float b, float a, String... names) {
        List<String> list = Arrays.asList(names);
        for (Group gr : this.groups) {
            if (list.stream().anyMatch(n -> n.equalsIgnoreCase(gr.name))) {
                renderGroup(gr, buf, m, r, g, b, a);
            }
        }
    }

    private static void renderGroup(Group gr, VertexConsumer buf, Matrix4f m, float r, float g, float b, float a) {
        for (Face f : gr.faces) {
            float au = 0.0F;
            float av = 0.0F;
            if (f.uvs != null) {
                for (float[] uv : f.uvs) {
                    au += uv[0];
                    av += uv[1];
                }
                au /= f.uvs.length;
                av /= f.uvs.length;
            }
            // веер треугольников (грани оригинала — треугольники и четырёхугольники)
            for (int i = 1; i + 1 < f.verts.length; i++) {
                vertex(buf, m, f, 0, au, av, r, g, b, a);
                vertex(buf, m, f, i, au, av, r, g, b, a);
                vertex(buf, m, f, i + 1, au, av, r, g, b, a);
            }
        }
    }

    private static void vertex(VertexConsumer buf, Matrix4f m, Face f, int i, float au, float av, float r, float g, float b, float a) {
        float[] v = f.verts[i];
        float u = 0.0F;
        float t = 0.0F;
        if (f.uvs != null) {
            float ou = 5.0E-4F;
            float ov = 5.0E-4F;
            if (f.uvs[i][0] > au) {
                ou = -ou;
            }
            if (f.uvs[i][1] > av) {
                ov = -ov;
            }
            u = f.uvs[i][0] + ou;
            t = f.uvs[i][1] + ov;
        }
        buf.addVertex(m, v[0], v[1], v[2]).setUv(u, t).setColor(r, g, b, a);
    }
}

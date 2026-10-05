package thaumcraft.client.lib.obj;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/** Загрузчик OBJ (порт MeshLoader): v, vt, vn, f (3–4 вершины), g/o, usemtl. Библиотеки материалов — при необходимости. */
public class MeshLoader {
    static final Set<String> unknownCommands = new HashSet<>();
    private MeshModel currentModel;
    private MeshPart currentPart;
    private String lastObjectName;

    private void addTexCoord(String line) {
        String[] args = line.split(" ");
        this.currentModel.addTexCoords(Float.parseFloat(args[0]), Float.parseFloat(args[1]));
    }

    private void addNormal(String line) {
        String[] args = line.split(" ");
        float x = Float.parseFloat(args[0]);
        float y = Float.parseFloat(args[1]);
        float z = args[2].equals("\\\\") ? (float) Math.sqrt(1.0F - x * x - y * y) : Float.parseFloat(args[2]);
        this.currentModel.addNormal(x, y, z);
    }

    private void addPosition(String line) {
        String[] args = line.split(" ");
        this.currentModel.addPosition(Float.parseFloat(args[0]), Float.parseFloat(args[1]), Float.parseFloat(args[2]));
    }

    private void addFace(String line) {
        String[] args = line.split(" ");
        if (args.length < 3 || args.length > 4) {
            throw new UnsupportedOperationException("OBJ: грань с " + args.length + " вершинами");
        }
        int[] v1 = parseIndices(args[0].split("/"));
        int[] v2 = parseIndices(args[1].split("/"));
        int[] v3 = parseIndices(args[2].split("/"));
        if (args.length == 3) {
            this.currentPart.addTriangleFace(v1, v2, v3);
        } else {
            this.currentPart.addQuadFace(v1, v2, v3, parseIndices(args[3].split("/")));
        }
    }

    private static int[] parseIndices(String[] p1) {
        int[] indices = new int[p1.length];
        for (int i = 0; i < p1.length; i++) {
            indices[i] = Integer.parseInt(p1[i]) - 1;
        }
        return indices;
    }

    private void useMaterial(String matName) {
        this.currentPart = new MeshPart();
        this.currentPart.name = this.lastObjectName;
        this.currentPart.material = matName;
        this.currentModel.addPart(this.currentPart);
    }

    public MeshModel loadFromResource(ResourceManager manager, ResourceLocation loc) throws IOException {
        this.currentModel = new MeshModel();
        try (BufferedReader lineReader = manager.getResourceOrThrow(loc).openAsReader()) {
            String currentLine;
            while ((currentLine = lineReader.readLine()) != null) {
                if (currentLine.isEmpty() || currentLine.startsWith("#")) {
                    continue;
                }
                if (currentLine.startsWith("v  ")) {
                    currentLine = currentLine.replaceFirst("v  ", "v ");
                }
                String[] fields = currentLine.split(" ", 2);
                String keyword = fields[0];
                String data = fields.length > 1 ? fields[1] : "";
                switch (keyword.toLowerCase()) {
                    case "o", "g" -> this.lastObjectName = data;
                    case "mtllib" -> {
                        // материалы читаются при запекании с материалами (bakeModel(ModelManager) оригинала) — этап 7+
                    }
                    case "usemtl" -> this.useMaterial(data);
                    case "v" -> this.addPosition(data);
                    case "vn" -> this.addNormal(data);
                    case "vt" -> this.addTexCoord(data);
                    case "f" -> this.addFace(data);
                    default -> unknownCommands.add(keyword);
                }
            }
        }
        return this.currentModel;
    }
}

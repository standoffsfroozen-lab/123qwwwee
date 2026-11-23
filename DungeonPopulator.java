package dev.cw.dungeon;

import dev.cw.CustomWorldsPlugin;
import dev.cw.dungeon.Loot.Table;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTable;

import java.util.Random;

/**
 * Редкие структуры: одна на квадрат заданного размера (в блоках), позиция внутри квадрата случайная.
 * Обычный мир: склеп, башня, павильон. Ад: бункер, кузня, алтарь. Энд: святилище, зловещее хранилище.
 */
public class DungeonPopulator extends BlockPopulator {
    private final boolean enabled;
    private final int crypt, tower, pavilion, bunker, forge, altar, shrine, vaultDungeon, endSafe;
    private final int fort, ziggurat, citadel, spire, grand;

    public DungeonPopulator(CustomWorldsPlugin plugin) {
        FileConfiguration c = plugin.getConfig();
        enabled = c.getBoolean("dungeons.enabled", true);
        crypt = c.getInt("dungeons.overworld-crypt", 3500);
        tower = c.getInt("dungeons.overworld-tower", 3000);
        pavilion = c.getInt("dungeons.overworld-pavilion", 2500);
        bunker = c.getInt("dungeons.nether-bunker", 1800);
        forge = c.getInt("dungeons.nether-forge", 700);
        altar = c.getInt("dungeons.nether-altar", 1500);
        shrine = c.getInt("dungeons.end-shrine", 1800);
        vaultDungeon = c.getInt("dungeons.end-vault", 700);
        endSafe = c.getInt("dungeons.end-safe-radius", 110);
        fort = c.getInt("dungeons.overworld-fort", 1300);
        ziggurat = c.getInt("dungeons.overworld-ziggurat", 1800);
        citadel = c.getInt("dungeons.nether-citadel", 900);
        spire = c.getInt("dungeons.end-spire", 800);
        grand = c.getInt("dungeons.end-grand", 1000);
    }

    /** true, если в этом чанке стоит структура данного типа (одна на квадрат spacing x spacing блоков). */
    private static boolean hit(WorldInfo info, int cx, int cz, int spacingBlocks, long salt) {
        if (spacingBlocks <= 0) return false;
        int s = Math.max(2, spacingBlocks / 16);
        int rx = Math.floorDiv(cx, s), rz = Math.floorDiv(cz, s);
        Random rr = new Random(info.getSeed() ^ (rx * 341873128712L) ^ (rz * 132897987541L) ^ (salt * 0x9E3779B97F4A7C15L));
        return rx * s + rr.nextInt(s) == cx && rz * s + rr.nextInt(s) == cz;
    }

    @Override
    public void populate(WorldInfo info, Random r, int cx, int cz, LimitedRegion g) {
        if (!enabled) return;
        switch (info.getEnvironment()) {
            case NORMAL -> {
                if (hit(info, cx, cz, crypt, 1)) crypt(g, r, cx, cz);
                if (hit(info, cx, cz, tower, 2)) tower(g, r, cx, cz);
                if (hit(info, cx, cz, pavilion, 3)) pavilion(g, r, cx, cz);
                if (hit(info, cx, cz, fort, 9)) fort(g, r, cx, cz);
                if (hit(info, cx, cz, ziggurat, 10)) ziggurat(g, r, cx, cz);
            }
            case NETHER -> {
                if (hit(info, cx, cz, bunker, 4)) bunker(g, r, cx, cz);
                if (hit(info, cx, cz, forge, 5)) forge(g, r, cx, cz);
                if (hit(info, cx, cz, altar, 6)) altar(g, r, cx, cz);
                if (hit(info, cx, cz, citadel, 11)) citadel(g, r, cx, cz);
            }
            case THE_END -> {
                if (hit(info, cx, cz, shrine, 7)) shrine(g, r, cx, cz);
                if (hit(info, cx, cz, vaultDungeon, 8)) vaultComplex(g, r, cx, cz);
                if (hit(info, cx, cz, spire, 12)) spire(g, r, cx, cz);
                if (hit(info, cx, cz, grand, 13)) grandVault(g, r, cx, cz);
            }
            default -> {}
        }
    }

    // ================================================================ ОБЫЧНЫЙ МИР

    private static final Material[] CRYPT_WALLS = {
            Material.STONE_BRICKS, Material.STONE_BRICKS, Material.MOSSY_STONE_BRICKS,
            Material.CRACKED_STONE_BRICKS, Material.MOSSY_COBBLESTONE};

    private void crypt(LimitedRegion g, Random r, int cx, int cz) {
        int x0 = cx * 16 + 3, z0 = cz * 16 + 3;
        int y0 = -40 + r.nextInt(70);

        room(g, r, x0, y0, z0, 9, 6, 9, CRYPT_WALLS);
        for (int dx : new int[]{2, 6})
            for (int dz : new int[]{2, 6})
                fill(g, x0 + dx, y0 + 1, z0 + dz, x0 + dx, y0 + 4, z0 + dz, Material.CHISELED_STONE_BRICKS);

        set(g, x0 + 4, y0 + 1, z0 + 4, Material.MOSSY_STONE_BRICKS);
        spawner(g, x0 + 4, y0 + 2, z0 + 4, r.nextBoolean() ? EntityType.ZOMBIE : EntityType.SKELETON);

        chest(g, x0 + 1, y0 + 1, z0 + 4, BlockFace.EAST, Table.O_CRYPT, r);
        chest(g, x0 + 7, y0 + 1, z0 + 4, BlockFace.WEST, r.nextBoolean() ? Table.O_CRYPT : Table.O_ARMORY, r);
        chest(g, x0 + 4, y0 + 1, z0 + 7, BlockFace.NORTH, Table.O_CRYPT_RARE, r);

        for (int i = 0; i < 14; i++)
            set(g, x0 + 1 + r.nextInt(7), y0 + 4, z0 + 1 + r.nextInt(7), Material.COBWEB);
    }

    private static final Material[] TOWER_WALLS = {
            Material.COBBLESTONE, Material.MOSSY_COBBLESTONE, Material.COBBLESTONE, Material.STONE_BRICKS};

    private void tower(LimitedRegion g, Random r, int cx, int cz) {
        int x0 = cx * 16 + 5, z0 = cz * 16 + 5;
        int base = groundLevel(g, x0 + 2, z0 + 2);
        if (base < 0) return;

        fill(g, x0, base - 4, z0, x0 + 4, base - 1, z0 + 4, Material.COBBLESTONE);
        room(g, r, x0, base, z0, 5, 9, 5, TOWER_WALLS);

        for (int x = x0; x <= x0 + 4; x++)
            for (int z = z0; z <= z0 + 4; z++) {
                boolean perimeter = x == x0 || x == x0 + 4 || z == z0 || z == z0 + 4;
                if (perimeter && r.nextInt(3) == 0) {
                    set(g, x, base + 8, z, Material.AIR);
                    if (r.nextBoolean()) set(g, x, base + 7, z, Material.AIR);
                }
                if (!perimeter && r.nextInt(2) == 0) set(g, x, base + 8, z, Material.AIR);
            }

        fill(g, x0 + 2, base + 1, z0 + 4, x0 + 2, base + 2, z0 + 4, Material.AIR);
        chest(g, x0 + 1, base + 1, z0 + 1, BlockFace.SOUTH, Table.O_TOWER, r);
        chest(g, x0 + 3, base + 1, z0 + 1, BlockFace.SOUTH, Table.O_ARMORY, r);
        spawner(g, x0 + 2, base + 1, z0 + 2, EntityType.SKELETON);
    }

    private void pavilion(LimitedRegion g, Random r, int cx, int cz) {
        int x0 = cx * 16 + 4, z0 = cz * 16 + 4;
        int xm = x0 + 3, zm = z0 + 3;
        int base = groundLevel(g, xm, zm);
        if (base < 0) return;

        Material[] floor = {Material.STONE_BRICKS, Material.MOSSY_STONE_BRICKS, Material.CRACKED_STONE_BRICKS};
        fill(g, x0, base - 3, z0, x0 + 6, base - 1, z0 + 6, Material.STONE);
        fillMix(g, r, x0, base, z0, x0 + 6, base, z0 + 6, floor);
        fill(g, x0, base + 1, z0, x0 + 6, base + 6, z0 + 6, Material.AIR);

        for (int dx : new int[]{0, 6})
            for (int dz : new int[]{0, 6}) {
                int hgt = 3 + r.nextInt(3);
                fill(g, x0 + dx, base + 1, z0 + dz, x0 + dx, base + hgt, z0 + dz, Material.MOSSY_STONE_BRICKS);
            }
        for (int x = x0; x <= x0 + 6; x++)
            for (int z = z0; z <= z0 + 6; z++)
                if ((x == x0 || x == x0 + 6 || z == z0 || z == z0 + 6) && r.nextInt(3) > 0)
                    set(g, x, base + 5, z, Material.STONE_BRICK_SLAB);

        set(g, xm, base + 1, zm, Material.CHISELED_STONE_BRICKS);
        chest(g, xm, base + 2, zm, BlockFace.SOUTH, Table.O_CAMP, r);
        // тайник под полом
        chest(g, xm, base - 2, zm, BlockFace.NORTH, Table.O_CRYPT_RARE, r);
    }

    /** Высота поверхности для постройки или -1, если вода/лава/дерево. */
    private static int groundLevel(LimitedRegion g, int x, int z) {
        if (!g.isInRegion(x, 64, z)) return -1;
        int top = g.getHighestBlockYAt(x, z, HeightMap.WORLD_SURFACE);
        Material m = g.getType(x, top, z);
        if (m == Material.WATER || m == Material.LAVA || m.isAir() || Tag.LEAVES.isTagged(m) || Tag.LOGS.isTagged(m)) return -1;
        return top;
    }

    // ================================================================ АД

    private static final Material[] BUNKER_WALLS = {
            Material.POLISHED_BLACKSTONE_BRICKS, Material.POLISHED_BLACKSTONE_BRICKS, Material.POLISHED_BLACKSTONE_BRICKS,
            Material.CRACKED_POLISHED_BLACKSTONE_BRICKS, Material.BLACKSTONE, Material.BLACKSTONE, Material.GILDED_BLACKSTONE};

    private void bunker(LimitedRegion g, Random r, int cx, int cz) {
        int x0 = cx * 16 + 3, z0 = cz * 16 + 3;
        int y = netherFloor(g, x0 + 4, z0 + 4);
        if (y < 0) return;

        fill(g, x0, y - 3, z0, x0 + 8, y - 1, z0 + 8, Material.BLACKSTONE);
        room(g, r, x0, y, z0, 9, 6, 9, BUNKER_WALLS);
        fill(g, x0, y + 1, z0 + 4, x0, y + 2, z0 + 4, Material.AIR);

        set(g, x0 + 4, y + 1, z0 + 4, Material.CHISELED_POLISHED_BLACKSTONE);
        spawner(g, x0 + 4, y + 2, z0 + 4, r.nextInt(3) == 0 ? EntityType.BLAZE : EntityType.WITHER_SKELETON);

        chest(g, x0 + 7, y + 1, z0 + 2, BlockFace.WEST, Table.N_ARMORY, r);
        chest(g, x0 + 7, y + 1, z0 + 6, BlockFace.WEST, Table.N_TREASURE, r);
        fill(g, x0 + 1, y, z0 + 6, x0 + 2, y, z0 + 7, Material.LAVA);
    }

    private static final Material[] FORGE_WALLS = {
            Material.NETHER_BRICKS, Material.NETHER_BRICKS, Material.RED_NETHER_BRICKS,
            Material.CRACKED_NETHER_BRICKS, Material.CHISELED_NETHER_BRICKS};

    private void forge(LimitedRegion g, Random r, int cx, int cz) {
        int x0 = cx * 16 + 2, z0 = cz * 16 + 2;
        int y = netherFloor(g, x0 + 5, z0 + 5);
        if (y < 0) return;

        fill(g, x0, y - 3, z0, x0 + 10, y - 1, z0 + 10, Material.NETHER_BRICKS);
        room(g, r, x0, y, z0, 11, 7, 11, FORGE_WALLS);
        // окна
        for (int i = 3; i <= 7; i += 2) {
            set(g, x0 + i, y + 3, z0, Material.NETHER_BRICK_FENCE);
            set(g, x0 + i, y + 3, z0 + 10, Material.NETHER_BRICK_FENCE);
        }
        fill(g, x0 + 5, y + 1, z0 + 10, x0 + 5, y + 3, z0 + 10, Material.AIR); // вход
        // лавовый горн в центре
        fill(g, x0 + 4, y, z0 + 4, x0 + 6, y, z0 + 6, Material.LAVA);
        for (int dx : new int[]{2, 8})
            for (int dz : new int[]{2, 8})
                fill(g, x0 + dx, y + 1, z0 + dz, x0 + dx, y + 5, z0 + dz, Material.RED_NETHER_BRICKS);
        for (int i = 0; i < 10; i++)
            set(g, x0 + 1 + r.nextInt(9), y, z0 + 1 + r.nextInt(9) , r.nextBoolean() ? Material.MAGMA_BLOCK : Material.NETHERRACK);
        fill(g, x0 + 4, y, z0 + 4, x0 + 6, y, z0 + 6, Material.LAVA);

        spawner(g, x0 + 5, y + 5, z0 + 1, EntityType.WITHER_SKELETON); // на полке у дальней стены
        set(g, x0 + 5, y + 4, z0 + 1, Material.NETHER_BRICKS);
        chest(g, x0 + 1, y + 1, z0 + 5, BlockFace.EAST, Table.N_ALCHEMY, r);
        chest(g, x0 + 9, y + 1, z0 + 5, BlockFace.WEST, Table.N_ARMORY, r);
        chest(g, x0 + 5, y + 1, z0 + 1, BlockFace.SOUTH, Table.N_TREASURE, r);
    }

    private void altar(LimitedRegion g, Random r, int cx, int cz) {
        int x0 = cx * 16 + 5, z0 = cz * 16 + 5;
        int y = netherFloor(g, x0 + 3, z0 + 3);
        if (y < 0) return;

        fill(g, x0, y - 2, z0, x0 + 6, y - 1, z0 + 6, Material.BLACKSTONE);
        fillMix(g, r, x0, y, z0, x0 + 6, y, z0 + 6, new Material[]{Material.POLISHED_BLACKSTONE, Material.POLISHED_BLACKSTONE_BRICKS, Material.GILDED_BLACKSTONE});
        fill(g, x0, y + 1, z0, x0 + 6, y + 5, z0 + 6, Material.AIR);
        for (int dx : new int[]{0, 6})
            for (int dz : new int[]{0, 6})
                fill(g, x0 + dx, y + 1, z0 + dz, x0 + dx, y + 3 + r.nextInt(2), z0 + dz, Material.POLISHED_BLACKSTONE_BRICKS);
        for (int dx : new int[]{2, 4})
            for (int dz : new int[]{2, 4}) {
                set(g, x0 + dx, y, z0 + dz, Material.SOUL_SOIL);
                set(g, x0 + dx, y + 1, z0 + dz, Material.SOUL_FIRE);
            }
        set(g, x0 + 3, y + 1, z0 + 3, Material.CHISELED_POLISHED_BLACKSTONE);
        chest(g, x0 + 3, y + 2, z0 + 3, BlockFace.SOUTH, r.nextBoolean() ? Table.N_TREASURE : Table.N_ALCHEMY, r);
    }

    private static int netherFloor(LimitedRegion g, int xc, int zc) {
        for (int y = 105; y >= 36; y--) {
            if (g.getType(xc, y, zc).isSolid() && g.getType(xc, y + 1, zc).isAir() && g.getType(xc, y + 2, zc).isAir()) return y;
        }
        return -1;
    }

    // ================================================================ ЭНД

    private static final Material[] SHRINE_WALLS = {
            Material.PURPUR_BLOCK, Material.PURPUR_BLOCK, Material.PURPUR_PILLAR, Material.END_STONE_BRICKS};

    private void shrine(LimitedRegion g, Random r, int cx, int cz) {
        int x0 = cx * 16 + 4, z0 = cz * 16 + 4;
        int xc = x0 + 3, zc = z0 + 3;
        if (Math.hypot(xc, zc) < endSafe) return;
        int y = endFloor(g, xc, zc);
        if (y < 0) return;

        fill(g, x0, y - 3, z0, x0 + 6, y - 1, z0 + 6, Material.END_STONE_BRICKS);
        room(g, r, x0, y, z0, 7, 6, 7, SHRINE_WALLS);
        fill(g, x0 + 3, y + 1, z0 + 6, x0 + 3, y + 2, z0 + 6, Material.AIR);

        for (int dx : new int[]{1, 5})
            for (int dz : new int[]{1, 5})
                set(g, x0 + dx, y + 1, z0 + dz, Material.END_ROD);

        set(g, x0 + 3, y + 1, z0 + 3, Material.PURPUR_PILLAR);
        spawner(g, x0 + 3, y + 2, z0 + 3, EntityType.ENDERMAN);
        chest(g, x0 + 1, y + 1, z0 + 3, BlockFace.EAST, Table.E_TREASURE, r);
        chest(g, x0 + 5, y + 1, z0 + 3, BlockFace.WEST, Table.E_ENCHANT, r);
    }

    private static final Material[] VAULT_WALLS = {
            Material.PURPUR_BLOCK, Material.PURPUR_PILLAR, Material.END_STONE_BRICKS, Material.END_STONE_BRICKS,
            Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.PURPUR_BLOCK};

    /** "Зловещее хранилище": 15x8x15, 3 зловещих сейфа, 4 спавнера по углам, сундук с ключами в центре. */
    private void vaultComplex(LimitedRegion g, Random r, int cx, int cz) {
        int x0 = cx * 16, z0 = cz * 16;
        int xc = x0 + 7, zc = z0 + 7;
        if (Math.hypot(xc, zc) < endSafe + 40) return;
        int y = endFloor(g, xc, zc);
        if (y < 0) return;

        int solid = 0;
        for (int[] c : new int[][]{{1, 1}, {13, 1}, {1, 13}, {13, 13}})
            if (hasStone(g, x0 + c[0], z0 + c[1], y)) solid++;
        if (solid < 3) return;

        fill(g, x0, y - 6, z0, x0 + 14, y - 1, z0 + 14, Material.END_STONE_BRICKS);
        room(g, r, x0, y, z0, 15, 8, 15, VAULT_WALLS);
        fill(g, x0 + 6, y + 1, z0 + 14, x0 + 8, y + 3, z0 + 14, Material.AIR); // вход с юга

        // колонны
        for (int dx : new int[]{3, 11})
            for (int dz : new int[]{4, 10})
                fill(g, x0 + dx, y + 1, z0 + dz, x0 + dx, y + 6, z0 + dz, Material.PURPUR_PILLAR);

        // спавнеры в углах на постаментах
        EntityType[] mobs = {EntityType.ENDERMAN, EntityType.ENDERMITE, EntityType.ENDERMAN, EntityType.SHULKER};
        int i = 0;
        for (int[] c : new int[][]{{1, 1}, {13, 1}, {1, 13}, {13, 13}}) {
            set(g, x0 + c[0], y + 1, z0 + c[1], Material.CRYING_OBSIDIAN);
            spawner(g, x0 + c[0], y + 2, z0 + c[1], mobs[i++]);
        }

        // три зловещих сейфа у северной стены
        for (int vx : new int[]{5, 7, 9}) trialVault(g, x0 + vx, y + 1, z0 + 1, true);

        // центральный постамент с сундуком ключей
        fill(g, x0 + 6, y + 1, z0 + 6, x0 + 8, y + 1, z0 + 8, Material.OBSIDIAN);
        chestKeys(g, x0 + 7, y + 2, z0 + 7, BlockFace.SOUTH, Table.E_RELICS, r, Material.OMINOUS_TRIAL_KEY, 1 + r.nextInt(2));

        // боковые сундуки
        chest(g, x0 + 1, y + 1, z0 + 7, BlockFace.EAST, Table.E_TREASURE, r);
        chest(g, x0 + 13, y + 1, z0 + 7, BlockFace.WEST, Table.E_ENCHANT, r);

        for (int k = 0; k < 8; k++)
            set(g, x0 + 1 + r.nextInt(13), y + 6, z0 + 1 + r.nextInt(13), Material.END_ROD);
    }

    private static int endFloor(LimitedRegion g, int xc, int zc) {
        for (int y = 108; y >= 22; y--)
            if (g.getType(xc, y, zc) == Material.END_STONE && g.getType(xc, y + 1, zc).isAir()) return y;
        return -1;
    }

    private static boolean hasStone(LimitedRegion g, int x, int z, int y) {
        for (int yy = y - 3; yy <= y + 3; yy++)
            if (g.getType(x, yy, z) == Material.END_STONE) return true;
        return false;
    }

    // ================================================================ СРЕДНИЕ И БОЛЬШИЕ СТРУКТУРЫ

    private static final Material[] FORT_WALLS = {
            Material.COBBLESTONE, Material.MOSSY_COBBLESTONE, Material.STONE_BRICKS,
            Material.MOSSY_STONE_BRICKS, Material.CRACKED_STONE_BRICKS};

    /** Средняя: разрушенный форт 21x21 — стены, 4 башни, центральный донжон. */
    private void fort(LimitedRegion g, Random r, int cx, int cz) {
        int x0 = cx * 16 - 2, z0 = cz * 16 - 2, x1 = x0 + 20, z1 = z0 + 20;
        int base = groundLevel(g, x0 + 10, z0 + 10);
        if (base < 0) return;

        fill(g, x0, base - 6, z0, x1, base - 1, z1, Material.COBBLESTONE);
        fillMix(g, r, x0, base, z0, x1, base, z1, new Material[]{Material.COBBLESTONE, Material.MOSSY_COBBLESTONE, Material.GRAVEL, Material.DIRT_PATH});
        fill(g, x0, base + 1, z0, x1, base + 12, z1, Material.AIR);

        for (int x = x0; x <= x1; x++)
            for (int z = z0; z <= z1; z++) {
                if (x != x0 && x != x1 && z != z0 && z != z1) continue;
                for (int y = base + 1; y <= base + 5; y++)
                    if (!(y >= base + 4 && r.nextInt(3) == 0)) set(g, x, y, z, FORT_WALLS[r.nextInt(FORT_WALLS.length)]);
            }
        fill(g, x0 + 9, base + 1, z1, x0 + 11, base + 4, z1, Material.AIR); // ворота

        int n = 0;
        for (int tx : new int[]{x0, x1 - 4})
            for (int tz : new int[]{z0, z1 - 4}) {
                room(g, r, tx, base, tz, 5, 11, 5, FORT_WALLS);
                for (int x = tx; x < tx + 5; x++)
                    for (int z = tz; z < tz + 5; z++)
                        if (r.nextInt(3) == 0) { set(g, x, base + 10, z, Material.AIR); if (r.nextBoolean()) set(g, x, base + 9, z, Material.AIR); }
                boolean west = tx == x0;
                fill(g, west ? tx + 4 : tx, base + 1, tz + 2, west ? tx + 4 : tx, base + 2, tz + 2, Material.AIR);
                chest(g, tx + 2, base + 1, tz + 2, west ? BlockFace.WEST : BlockFace.EAST, (n++ % 2 == 0) ? Table.O_CAMP : Table.O_TOWER, r);
            }

        int kx = x0 + 6, kz = z0 + 6;
        room(g, r, kx, base, kz, 9, 8, 9, FORT_WALLS);
        for (int x = kx; x < kx + 9; x++)
            for (int z = kz; z < kz + 9; z++)
                if (r.nextInt(2) == 0) set(g, x, base + 7, z, Material.AIR);
        fill(g, kx + 4, base + 1, kz + 8, kx + 4, base + 2, kz + 8, Material.AIR);
        set(g, kx + 4, base + 1, kz + 4, Material.MOSSY_STONE_BRICKS);
        spawner(g, kx + 4, base + 2, kz + 4, r.nextBoolean() ? EntityType.ZOMBIE : EntityType.SKELETON);
        chest(g, kx + 1, base + 1, kz + 4, BlockFace.EAST, Table.O_ARMORY, r);
        chest(g, kx + 7, base + 1, kz + 4, BlockFace.WEST, Table.O_CRYPT_RARE, r);
        chest(g, kx + 4, base + 1, kz + 1, BlockFace.SOUTH, Table.O_TOWER, r);
    }

    private static final Material[] ZIG_MATS = {
            Material.STONE_BRICKS, Material.MOSSY_STONE_BRICKS, Material.CRACKED_STONE_BRICKS,
            Material.STONE_BRICKS, Material.CHISELED_STONE_BRICKS, Material.MOSSY_COBBLESTONE};

    /** Большая: ступенчатая зиккурат-пирамида 31x31, внутри зал с сейфами, спавнерами и ключами. */
    private void ziggurat(LimitedRegion g, Random r, int cx, int cz) {
        int x0 = cx * 16 - 7, z0 = cz * 16 - 7;
        int base = groundLevel(g, x0 + 15, z0 + 15);
        if (base < 0) return;

        fillMix(g, r, x0, base - 8, z0, x0 + 30, base, z0 + 30, new Material[]{Material.COBBLESTONE, Material.MOSSY_COBBLESTONE, Material.STONE});
        for (int i = 0; i < 5; i++) {
            int size = 31 - 6 * i, off = 3 * i, yb = base + 1 + 3 * i;
            fillMix(g, r, x0 + off, yb, z0 + off, x0 + off + size - 1, yb + 2, z0 + off + size - 1, ZIG_MATS);
        }
        // зал внутри
        fill(g, x0 + 9, base + 1, z0 + 9, x0 + 21, base + 6, z0 + 21, Material.AIR);
        fill(g, x0 + 14, base + 1, z0 + 22, x0 + 16, base + 3, z0 + 30, Material.AIR); // вход с юга
        for (int dx : new int[]{11, 19})
            for (int dz : new int[]{11, 19})
                fill(g, x0 + dx, base + 1, z0 + dz, x0 + dx, base + 6, z0 + dz, Material.CHISELED_STONE_BRICKS);

        EntityType[] mobs = {EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CAVE_SPIDER};
        int i = 0;
        for (int[] c : new int[][]{{10, 10}, {20, 10}, {10, 20}, {20, 20}}) {
            set(g, x0 + c[0], base + 1, z0 + c[1], Material.MOSSY_STONE_BRICKS);
            spawner(g, x0 + c[0], base + 2, z0 + c[1], mobs[i++]);
        }
        fill(g, x0 + 14, base + 1, z0 + 14, x0 + 16, base + 1, z0 + 16, Material.POLISHED_ANDESITE);
        chestKeys(g, x0 + 15, base + 2, z0 + 15, BlockFace.SOUTH, Table.O_CRYPT_RARE, r, Material.TRIAL_KEY, 1 + r.nextInt(2));
        trialVault(g, x0 + 13, base + 1, z0 + 9, false);
        trialVault(g, x0 + 17, base + 1, z0 + 9, false);
        chest(g, x0 + 9, base + 1, z0 + 15, BlockFace.EAST, Table.O_ARMORY, r);
        chest(g, x0 + 21, base + 1, z0 + 15, BlockFace.WEST, Table.O_TOWER, r);
        for (int k = 0; k < 18; k++)
            set(g, x0 + 10 + r.nextInt(11), base + 6, z0 + 10 + r.nextInt(11), Material.COBWEB);
        // сундук на вершине
        chest(g, x0 + 15, base + 16, z0 + 15, BlockFace.SOUTH, Table.O_CRYPT_RARE, r);
    }

    /** Большая (ад): Цитадель 31x31 — стены с зубцами, 4 башни, донжон с лавой, спавнерами и сундуками. */
    private void citadel(LimitedRegion g, Random r, int cx, int cz) {
        int x0 = cx * 16 - 7, z0 = cz * 16 - 7, x1 = x0 + 30, z1 = z0 + 30;
        int y = netherFloor(g, x0 + 15, z0 + 15);
        if (y < 0 || y > 100) return;

        fill(g, x0, y - 8, z0, x1, y - 1, z1, Material.NETHER_BRICKS);
        fill(g, x0, y + 1, z0, x1, y + 17, z1, Material.AIR);
        fillMix(g, r, x0, y, z0, x1, y, z1, FORGE_WALLS);

        for (int x = x0; x <= x1; x++)
            for (int z = z0; z <= z1; z++) {
                if (x != x0 && x != x1 && z != z0 && z != z1) continue;
                for (int yy = y + 1; yy <= y + 7; yy++) set(g, x, yy, z, FORGE_WALLS[r.nextInt(FORGE_WALLS.length)]);
                if ((x + z) % 2 == 0) set(g, x, y + 8, z, Material.NETHER_BRICKS);
            }
        fill(g, x0 + 13, y + 1, z1, x0 + 17, y + 5, z1, Material.AIR); // ворота
        for (int x = x0 + 13; x <= x0 + 17; x += 2) set(g, x, y + 5, z1, Material.NETHER_BRICK_FENCE);

        int n = 0;
        Table[] tt = {Table.N_ARMORY, Table.N_ALCHEMY, Table.N_TREASURE, Table.N_ARMORY};
        for (int tx : new int[]{x0, x1 - 6})
            for (int tz : new int[]{z0, z1 - 6}) {
                room(g, r, tx, y, tz, 7, 15, 7, FORGE_WALLS);
                boolean west = tx == x0;
                fill(g, west ? tx + 6 : tx, y + 1, tz + 3, west ? tx + 6 : tx, y + 2, tz + 3, Material.AIR);
                chest(g, tx + 3, y + 1, tz + 3, west ? BlockFace.WEST : BlockFace.EAST, tt[n++ % 4], r);
                set(g, tx + 3, y + 4, tz + 1, Material.NETHER_BRICKS);
                spawner(g, tx + 3, y + 5, tz + 1, EntityType.BLAZE);
                fill(g, tx + 1, y + 7, tz + 1, tx + 5, y + 7, tz + 5, Material.NETHER_BRICKS);
            }

        int kx = x0 + 8, kz = z0 + 8;
        room(g, r, kx, y, kz, 15, 11, 15, FORGE_WALLS);
        fill(g, kx + 6, y + 1, kz + 14, kx + 8, y + 4, kz + 14, Material.AIR);
        fill(g, kx + 5, y, kz + 5, kx + 9, y, kz + 9, Material.LAVA);
        fill(g, kx + 7, y, kz + 7, kx + 7, y, kz + 7, Material.OBSIDIAN);
        for (int dx : new int[]{3, 11})
            for (int dz : new int[]{3, 11})
                fill(g, kx + dx, y + 1, kz + dz, kx + dx, y + 9, kz + dz, Material.RED_NETHER_BRICKS);
        spawner(g, kx + 2, y + 1, kz + 2, EntityType.WITHER_SKELETON);
        spawner(g, kx + 12, y + 1, kz + 2, EntityType.WITHER_SKELETON);
        chest(g, kx + 7, y + 1, kz + 1, BlockFace.SOUTH, Table.N_TREASURE, r);
        chest(g, kx + 1, y + 1, kz + 7, BlockFace.EAST, Table.N_ALCHEMY, r);
        chest(g, kx + 13, y + 1, kz + 7, BlockFace.WEST, Table.N_ARMORY, r);
        chest(g, kx + 7, y + 1, kz + 12, BlockFace.NORTH, Table.N_TREASURE, r);
    }

    /** Средняя (энд): шпиль 9x9x30 с лестницей-лазом, 3 этажа, спавнеры и сундуки. */
    private void spire(LimitedRegion g, Random r, int cx, int cz) {
        int x0 = cx * 16 + 3, z0 = cz * 16 + 3;
        if (Math.hypot(x0 + 4, z0 + 4) < endSafe + 40) return;
        int y = endFloor(g, x0 + 4, z0 + 4);
        if (y < 0 || y > 75) return;

        fill(g, x0, y - 6, z0, x0 + 8, y - 1, z0 + 8, Material.END_STONE_BRICKS);
        room(g, r, x0, y, z0, 9, 30, 9, SHRINE_WALLS);
        for (int fy : new int[]{y + 9, y + 18})
            fill(g, x0 + 1, fy, z0 + 1, x0 + 7, fy, z0 + 7, Material.PURPUR_BLOCK);
        // лаз: проём в перекрытиях и лестница
        for (int yy = y + 1; yy <= y + 27; yy++) {
            set(g, x0 + 1, yy, z0 + 4, Material.AIR);
            ladder(g, x0 + 1, yy, z0 + 4, BlockFace.EAST);
        }
        fill(g, x0 + 4, y + 1, z0 + 8, x0 + 4, y + 2, z0 + 8, Material.AIR); // вход с юга

        spawner(g, x0 + 4, y + 1, z0 + 4, EntityType.ENDERMAN);
        chest(g, x0 + 7, y + 1, z0 + 4, BlockFace.WEST, Table.E_TREASURE, r);
        spawner(g, x0 + 4, y + 10, z0 + 4, EntityType.SHULKER);
        chest(g, x0 + 7, y + 10, z0 + 4, BlockFace.WEST, Table.E_ENCHANT, r);
        chest(g, x0 + 7, y + 19, z0 + 4, BlockFace.WEST, Table.E_RELICS, r);
        set(g, x0 + 4, y + 19, z0 + 4, Material.CRYING_OBSIDIAN);
        spawner(g, x0 + 4, y + 20, z0 + 4, EntityType.ENDERMAN);
        for (int fy : new int[]{y + 8, y + 17, y + 26})
            for (int dx : new int[]{1, 7})
                for (int dz : new int[]{1, 7}) set(g, x0 + dx, fy, z0 + dz, Material.END_ROD);
    }

    /** Большая (энд): Великое зловещее хранилище 31x31 — 5 зловещих сейфов, 6 спавнеров, много сундуков. */
    private void grandVault(LimitedRegion g, Random r, int cx, int cz) {
        int x0 = cx * 16 - 7, z0 = cz * 16 - 7;
        if (Math.hypot(x0 + 15, z0 + 15) < endSafe + 60) return;
        int y = endFloor(g, x0 + 15, z0 + 15);
        if (y < 0 || y > 85) return;
        int solid = 0;
        for (int[] c : new int[][]{{2, 2}, {28, 2}, {2, 28}, {28, 28}})
            if (hasStone(g, x0 + c[0], z0 + c[1], y)) solid++;
        if (solid < 3) return;

        fill(g, x0, y - 8, z0, x0 + 30, y - 1, z0 + 30, Material.END_STONE_BRICKS);
        room(g, r, x0, y, z0, 31, 13, 31, VAULT_WALLS);
        fill(g, x0 + 14, y + 1, z0 + 30, x0 + 16, y + 4, z0 + 30, Material.AIR); // вход

        // внутренняя камера с сейфами
        room(g, r, x0 + 9, y, z0 + 9, 13, 10, 13, VAULT_WALLS);
        fill(g, x0 + 14, y + 1, z0 + 21, x0 + 16, y + 3, z0 + 21, Material.AIR);
        for (int vx : new int[]{11, 13, 15, 17, 19}) trialVault(g, x0 + vx, y + 1, z0 + 10, true);
        fill(g, x0 + 14, y + 1, z0 + 14, x0 + 16, y + 1, z0 + 16, Material.OBSIDIAN);
        chestKeys(g, x0 + 15, y + 2, z0 + 15, BlockFace.SOUTH, Table.E_RELICS, r, Material.OMINOUS_TRIAL_KEY, 2 + r.nextInt(2));
        for (int sx : new int[]{10, 20}) {
            set(g, x0 + sx, y + 1, z0 + 20, Material.CRYING_OBSIDIAN);
            spawner(g, x0 + sx, y + 2, z0 + 20, EntityType.ENDERMAN);
        }

        // внешний зал: спавнеры по углам и у стен, колонны, сундуки
        EntityType[] mobs = {EntityType.ENDERMAN, EntityType.ENDERMITE, EntityType.SHULKER, EntityType.ENDERMAN};
        int i = 0;
        for (int[] c : new int[][]{{2, 2}, {28, 2}, {2, 28}, {28, 28}}) {
            set(g, x0 + c[0], y + 1, z0 + c[1], Material.CRYING_OBSIDIAN);
            spawner(g, x0 + c[0], y + 2, z0 + c[1], mobs[i++]);
        }
        for (int dx : new int[]{5, 25})
            for (int dz : new int[]{5, 15, 25})
                fill(g, x0 + dx, y + 1, z0 + dz, x0 + dx, y + 11, z0 + dz, Material.PURPUR_PILLAR);

        chest(g, x0 + 2, y + 1, z0 + 15, BlockFace.EAST, Table.E_TREASURE, r);
        chest(g, x0 + 28, y + 1, z0 + 15, BlockFace.WEST, Table.E_ENCHANT, r);
        chest(g, x0 + 15, y + 1, z0 + 2, BlockFace.SOUTH, Table.E_RELICS, r);
        chest(g, x0 + 8, y + 1, z0 + 2, BlockFace.SOUTH, Table.E_TREASURE, r);
        chest(g, x0 + 22, y + 1, z0 + 2, BlockFace.SOUTH, Table.E_ENCHANT, r);
        for (int k = 0; k < 24; k++)
            set(g, x0 + 1 + r.nextInt(29), y + 11, z0 + 1 + r.nextInt(29), Material.END_ROD);
    }

    private static void ladder(LimitedRegion g, int x, int y, int z, BlockFace face) {
        if (!g.isInRegion(x, y, z)) return;
        g.setType(x, y, z, Material.LADDER);
        BlockData bd = g.getBlockData(x, y, z);
        if (bd instanceof Directional d) {
            d.setFacing(face);
            g.setBlockData(x, y, z, d);
        }
    }

    // ================================================================ ПОМОЩНИКИ

    private static void set(LimitedRegion g, int x, int y, int z, Material m) {
        if (g.isInRegion(x, y, z)) g.setType(x, y, z, m);
    }

    private static void fill(LimitedRegion g, int x1, int y1, int z1, int x2, int y2, int z2, Material m) {
        for (int x = x1; x <= x2; x++)
            for (int y = y1; y <= y2; y++)
                for (int z = z1; z <= z2; z++) set(g, x, y, z, m);
    }

    private static void fillMix(LimitedRegion g, Random r, int x1, int y1, int z1, int x2, int y2, int z2, Material[] mats) {
        for (int x = x1; x <= x2; x++)
            for (int y = y1; y <= y2; y++)
                for (int z = z1; z <= z2; z++) set(g, x, y, z, mats[r.nextInt(mats.length)]);
    }

    private static void room(LimitedRegion g, Random r, int x0, int y0, int z0, int sx, int sy, int sz, Material[] walls) {
        fillMix(g, r, x0, y0, z0, x0 + sx - 1, y0 + sy - 1, z0 + sz - 1, walls);
        fill(g, x0 + 1, y0 + 1, z0 + 1, x0 + sx - 2, y0 + sy - 2, z0 + sz - 2, Material.AIR);
    }

    private static void chest(LimitedRegion g, int x, int y, int z, BlockFace face, Table table, Random r) {
        chestKeys(g, x, y, z, face, table, r, Material.TRIAL_KEY, 0);
    }

    private static void chestKeys(LimitedRegion g, int x, int y, int z, BlockFace face, Table table, Random r, Material keyMat, int keys) {
        if (!g.isInRegion(x, y, z)) return;
        g.setType(x, y, z, Material.CHEST);
        BlockData bd = g.getBlockData(x, y, z);
        if (bd instanceof Directional d) {
            d.setFacing(face);
            g.setBlockData(x, y, z, d);
        }
        BlockState st = g.getBlockState(x, y, z);
        if (st instanceof org.bukkit.block.Chest c) {
            Loot.fill(c.getBlockInventory(), table, r);
            if (keys > 0) c.getBlockInventory().addItem(new ItemStack(keyMat, keys));
            c.update(true, false);
        }
    }

    private static void spawner(LimitedRegion g, int x, int y, int z, EntityType type) {
        if (!g.isInRegion(x, y, z)) return;
        g.setType(x, y, z, Material.SPAWNER);
        BlockState st = g.getBlockState(x, y, z);
        if (st instanceof CreatureSpawner cs) {
            cs.setSpawnedType(type);
            cs.update(true, false);
        }
    }

    /** Зловещий сейф (Vault): открывается зловещим ключом из сундука, лут — ванильный из зловещих испытаний. */
    private static void trialVault(LimitedRegion g, int x, int y, int z, boolean ominous) {
        if (!g.isInRegion(x, y, z)) return;
        g.setType(x, y, z, Material.VAULT);
        BlockData bd = g.getBlockData(x, y, z);
        if (bd instanceof org.bukkit.block.data.type.Vault v) {
            v.setOminous(ominous);
            v.setFacing(BlockFace.SOUTH);
            g.setBlockData(x, y, z, v);
        }
        BlockState st = g.getBlockState(x, y, z);
        if (st instanceof org.bukkit.block.Vault vs) {
            vs.setKeyItem(new ItemStack(ominous ? Material.OMINOUS_TRIAL_KEY : Material.TRIAL_KEY));
            LootTable lt = Bukkit.getLootTable(NamespacedKey.minecraft(ominous ? "chests/trial_chambers/reward_ominous" : "chests/trial_chambers/reward"));
            if (lt != null) vs.setLootTable(lt);
            vs.update(true, false);
        }
    }
}

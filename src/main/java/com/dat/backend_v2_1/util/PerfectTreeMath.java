package com.dat.backend_v2_1.util;

public class PerfectTreeMath {
    /**
     * Tìm lũy thừa của 2 gần nhất >= n
     * VD: Input 5 -> Output 8
     */
    public static int getCapacity(int numberOfPlayers) {
        if (numberOfPlayers < 2) return 2;
        int capacity = 1;
        while (capacity < numberOfPlayers) {
            capacity *= 2;
        }
        return capacity;
    }

    /**
     * Lấy index của người chơi trong mảng cây
     * playerIndexInList: Thứ tự trong danh sách user (0, 1, 2...)
     */
    public static int getLeafIndex(int playerIndexInList, int capacity) {
        // Người chơi bắt đầu từ vị trí (capacity - 1)
        return (capacity - 1) + playerIndexInList;
    }

    // --- Các phép tính điều hướng ---

    public static int getParent(int index) {
        if (index == 0) return -1;
        return (index - 1) / 2;
    }

    public static int getLeftChild(int index) {
        return 2 * index + 1;
    }

    public static int getRightChild(int index) {
        return 2 * index + 2;
    }

    /**
     * Kiểm tra xem index này là Slot chứa người chơi hay Slot trận đấu
     */
    public static boolean isPlayerSlot(int index, int capacity) {
        return index >= (capacity - 1);
    }

    /**
     * Lấy Level của node (Chung kết là level 1 hoặc max tùy quy ước)
     * Ở đây ta tính: Root = 0, càng xuống càng tăng
     */
    public static int getLevel(int index) {
        return (int) (Math.floor(Math.log(index + 1) / Math.log(2)));
    }
}

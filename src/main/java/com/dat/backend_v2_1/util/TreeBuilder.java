package com.dat.backend_v2_1.util;

import java.util.ArrayList;
import java.util.List;

public class TreeBuilder {
    public static List<Integer> getBalancedLeafIndices(int numberOfPlayers) {
        List<Integer> resultIndices = new ArrayList<>();

        // 1. Tính capacity (Lũy thừa 2 gần nhất >= n)
        int capacity = 1;
        while (capacity < numberOfPlayers) {
            capacity *= 2;
        }

        // 2. Bắt đầu chia đệ quy từ Root (index 0)
        // Root quản lý toàn bộ 'numberOfPlayers' người
        // Root có tầm cover là 'capacity' slot
        distribute(0, numberOfPlayers, capacity, resultIndices);

        return resultIndices;
    }

    /**
     * @param currentIndex: Index của node hiện tại đang xét (theo công thức 2i+1)
     * @param playersInNode: Số lượng người thực tế cần nhét vào nhánh này
     * @param capacityOfNode: Sức chứa tối đa của nhánh này (luôn là lũy thừa 2)
     * @param result: List lưu kết quả
     */
    private static void distribute(int currentIndex, int playersInNode, int capacityOfNode, List<Integer> result) {
        // Nếu capacity = 1, tức là đã đến Node lá
        if (capacityOfNode == 1) {
            // Nếu tại đây có người (playersInNode = 1), thì thêm index này vào kết quả
            if (playersInNode == 1) {
                result.add(currentIndex);
            }
            return;
        }

        // --- CHIA ĐÔI ĐỂ CÂN BẰNG ---

        // Sức chứa của mỗi con giảm đi một nửa
        int childCapacity = capacityOfNode / 2;

        // Số người chia cho con trái (Ưu tiên dư sang trái)
        // Ví dụ: 13 người -> Trái 7, Phải 6
        int leftPlayers = (int) Math.ceil((double) playersInNode / 2);

        // Số người chia cho con phải
        int rightPlayers = playersInNode - leftPlayers;

        // Đệ quy xuống con trái
        distribute(2 * currentIndex + 1, leftPlayers, childCapacity, result);

        // Đệ quy xuống con phải
        distribute(2 * currentIndex + 2, rightPlayers, childCapacity, result);
    }
}

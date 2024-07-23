package utility;

import customExceptions.OrderStateUndefinedExpection;

import java.util.HashMap;
import java.util.Map;

public class Translator {
    public static Map<Integer, String[]> getMapOfDishName(){
        Map<Integer, String[]> fieldsMap = new HashMap<>();
        fieldsMap.put(1, new String[]{"name", "菜品名"});
        fieldsMap.put(2, new String[]{"type", "菜品类型"});
        fieldsMap.put(3, new String[]{"description", "菜品描述"});
        fieldsMap.put(4, new String[]{"ingredients", "食材"});
        fieldsMap.put(5, new String[]{"possible_allergens", "可能的过敏原"});
        fieldsMap.put(6, new String[]{"nutrition", "营养成分"});
        fieldsMap.put(7, new String[]{"image", "图片"});
        fieldsMap.put(8, new String[]{"rating", "评分"});
        return fieldsMap;
    }

    public static String translateOrderState(int state) throws OrderStateUndefinedExpection {
        switch (state) {
            case Constants.UNFINISHED:
                return "待出餐";
            case Constants.FINISHED:
                return "已出餐";
            case Constants.INFORMED:
                return "用户已收到出餐通知";
            case Constants.COMMENTED:
                return "用户已评价";
            default:
                throw new OrderStateUndefinedExpection(state);
        }
    }
}

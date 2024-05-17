package twomillions.plugin.advancedwish.enums.wish;

/**
 * @author 2000000
 * @date 2023/02/02
 */
public enum PlayerWishStatus {

    /**
     * Allow - 允许许愿
     */
    Allow,

    /**
     * InProgress - 正在进行许愿
     */
    InProgress,

    /**
     * RequirementsNotMet - 未满足许愿要求
     */
    RequirementsNotMet,

    /**
     * ReachLimit - 到达许愿限制
     */
    ReachLimit,

    /**
     * LoadingCache - 正在处理缓存数据
     */
    LoadingCache,

    /**
     * WaitingLoadingCache - 等待处理缓存信息
     */
    WaitingLoadingCache
}

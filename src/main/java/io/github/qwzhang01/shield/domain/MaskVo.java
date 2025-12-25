package io.github.qwzhang01.shield.domain;

import java.io.Serializable;

/**
 * Base class for objects that support row-level masking control.
 *
 * <p>Classes extending MaskVo can control masking behavior at the
 * instance level using the maskFlag property. This is particularly useful
 * for scenarios where:</p>
 * <ul>
 *   <li>Different rows in a list require different masking rules</li>
 *   <li>The same object needs different masking behavior in different
 *   contexts</li>
 *   <li>Conditional masking based on user permissions or data ownership</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>
 * public class UserInfo extends MaskVo {
 *     {@code @MaskPhone}
 *     private String phone;
 *
 *     {@code @MaskEmail}
 *     private String email;
 * }
 *
 * // Enable masking for this instance
 * UserInfo user = new UserInfo();
 * user.setMaskFlag(true); // This instance will be masked
 *
 * // Disable masking for another instance
 * UserInfo owner = new UserInfo();
 * owner.setMaskFlag(false); // This instance will NOT be masked
 * </pre>
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class MaskVo implements Serializable {
    /**
     * Flag indicating whether this instance should be masked.
     * - true: Apply masking to annotated fields
     * - false: Skip masking for this instance
     * - null: Use default masking behavior
     */
    private Boolean maskFlag;

    public Boolean getMaskFlag() {
        return maskFlag;
    }

    public void setMaskFlag(Boolean maskFlag) {
        this.maskFlag = maskFlag;
    }
}

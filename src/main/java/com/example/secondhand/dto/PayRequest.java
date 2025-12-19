/*
 * @Author: 'txy' '841067099@qq.com'
 * @Date: 2025-12-17 11:32:38
 * @LastEditors: 'txy' '841067099@qq.com'
 * @LastEditTime: 2025-12-17 11:32:46
 * @FilePath: \secondhand-try\backend\secondhand\src\main\java\com\example\secondhand\dto\PayRequest.java
 * @Description: 这是默认设置,请设置`customMade`, 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 */
package com.example.secondhand.dto;

import lombok.Data;

@Data
public class PayRequest {

    // 商户订单号（你自己系统里的订单号，不能重复）
    private String orderId;

    // 支付金额（字符串，比如 "88.88"）
    private String amount;

    // 商品标题
    private String title;
}

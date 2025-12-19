package com.example.secondhand.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.AlipayConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlipayClientConfig {

    @Bean
    public AlipayClient alipayClient() throws Exception {

        AlipayConfig config = new AlipayConfig();

        // 1️⃣ 沙箱网关（不要改）
        config.setServerUrl("https://openapi-sandbox.dl.alipaydev.com/gateway.do");

        // 🔴 2. 你的沙箱 AppId
        config.setAppId("9021000158648989");

        // 🔴 3. 商户私钥（就是你贴的那一大串）
        config.setPrivateKey("MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCXOrG37k6vrUUmne1UMYbXxDTfR3B1cvHRChtJWmX0+89baneac5AIotVMyrCRWmVSJ2d7+bCpl8aBhZRnqR7PXqTN899r/3MN89JvSBSXRlbpgkJm3YozrhQEgdaKFdYE472HDT6hSo31D1myAs6OH7kSALxIzGznmsIYiQ2VUGZm61AsDFsMuLk5Ry65Mr626yY2+lJXL1zeJEJivjO5U3offaJnHY1tVduHlFMMEIYFWrLUdJV6OK9vdzLTCcpftn5DVkbH76ccJVST30kMI52571bvEmE6H7v8lJpyCFyUr1RjPZojJ8jYnHTqP6K3XtKGLFtlxDxllnj/Y3g5AgMBAAECggEAbPwxo3osXtKiKShbJvfiU5DlAZw8YYgK2TlNXNm27IykbR8XPsnVnUOsMIFKKhZZOpGHNINHW5Ezz5fy0ZnByHROlNYRx4T65601lFj/7d0VOYdUT0mNFZ14c58We8Es0a1GoxNadWaDU35Sh8UaErat61l4V4ZrXaXcB06N6XJAfxojaPXO0xjrs11t9nJibLqyOjZcsIu9ohq0xe1jccFGGJwdoLyzohGzq8woHag4wq6uSxrmc4z3fQ5VC3mZOA6O6OL8soicW9fT8volItwwaDf0TLC/EfqCsTzLGzBhVBQknJth7UcHnRCxO2BYGIecoObXIEL7MZN6eKMeOQKBgQDbBdBe5uJ/rHNoPvHT63mKpgLDP8VvpWFwutT0WCva8TL5nUeoLLlUNaTMO4wbNIunbPCP7fOoEWqU64+g9SxLwK/v31n63lKD+2kG94mTBqqt2GrKZtY0yeKon9p9jZfELzo1dvSrg0wM6k+i6Pd1Si6zi/tWs44o4ZXe9pIrewKBgQCwwtchxpfhB6lOLfBxiXVhTDXeqZW9uSmtDrjB03JONLwWLJR+vRKf4mbTMFMVZD0UvTXj7b/ewCEHPZf3iphnZw6Vdi0zWbWDR//D4H0ridxvjE8XX+nO6bWbt1tgEkmRAutrj83Vp7DdnKtAO0U7TvzcS1B3eFf9DQuPrlry2wKBgGxOe7PySJ8KECfEuKErGHdPkJ4sst160qyENzp9P3KNQ7/b3stzElJqFIxKBgaN/WFVpcAG0y6RLjtmIShfFQCA0H+12zELL5LEiDNBxW5HJa/CDUz5fYOtMcBhDBJ6KkqBcC9wG52U186rL6ZHvdbqUB3JYwiE0g+1Pf97vtxxAoGBAIPCeULAGm6vm1Cfhd/cKxdAC9fSacBmyd4qXyZ8S3RoctvEl1xx6/Qqe6NpHkWP1sgi0oz4qFGOvg/7pc/sC/boJ0LaOI9Si680eD62lBdkSGEOIda2tD58j9dnMkHG3/esSXiAVVqyCXDoW+8hP1YaZvp/WZf0qQIEO137BeiJAoGBANTMXtgKu+mIgKLs5qGQPez3zV2gHz5Mo0hOizkCWRNUj0fvkG3NCDRw5mzXOqsnhh7vr9JMhvHB94daX5FEb0xBWxMsawv4swSNxewT9Fp9S4BjefUnbVc9hWdIxAzyxOsE9neUin6iCm5j+oGc54X1mMQoMN4mFs/N5BEW/i9F");

        // 🔴 4. 支付宝公钥
        config.setAlipayPublicKey("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmoAMdCwoU7bxQpnEMgGD9AXDLahcT0HseNZbWNB8kEhESAvxXBX0d1Dy+SagTkp8a8c3VMCZf1sJU8txFZ42efnqglg0tP196WRG8PP5OuJGx++UdLXYFakXlVq2zVe+BWynXHGIe9Porv+R523hXoawH5oJqE0f6ztHtujNWjkGIOUJ9URCA0G84h0L0ICTY3khSo8iBttP2nUlmKrKFh556cNkBvGSbNxx6/F7K7CN5kbRX6gjw3hi9/RXG75gdz1Le0J3nfm+A1PYdqlRMs1W/Hqe8ULXSDwRT62zMiHyXReqb6UQHRkkohh01Xbo65lKLN8MkkOCe64JwCPdmQIDAQAB");

        config.setFormat("json");
        config.setCharset("UTF-8");
        config.setSignType("RSA2");

        return new DefaultAlipayClient(config);
    }
}

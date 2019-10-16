/*
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.rosco.providers.huaweicloud.config

import com.netflix.spinnaker.rosco.api.BakeOptions
import com.netflix.spinnaker.rosco.api.BakeRequest
import com.netflix.spinnaker.rosco.providers.huaweicloud.HuaweiCloudBakeHandler
import com.netflix.spinnaker.rosco.providers.registry.CloudProviderBakeHandlerRegistry
import groovy.transform.AutoClone
import groovy.transform.AutoCloneStyle
import javax.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


@Configuration
@ConditionalOnProperty('huaweicloud.enabled')
@ComponentScan('com.netflix.spinnaker.rosco.providers.huaweicloud')
class RoscoHuaweiCloudConfiguration {

  @Autowired
  CloudProviderBakeHandlerRegistry cloudProviderBakeHandlerRegistry

  @Autowired
  HuaweiCloudBakeHandler huaweicloudBakeHandler

  @Bean
  @ConfigurationProperties('huaweicloud.bakery-defaults')
  HuaweiCloudBakeryDefaults huaweicloudBakeryDefaults() {
    new HuaweiCloudBakeryDefaults()
  }

  static class HuaweiCloudBakeryDefaults {
    String authUrl
    String domainName
    String floatingIpPool
    String networkId
    String securityGroups
    String projectName
    String templateFile
    Boolean insecure
    String username
    String password
    List<HuaweiCloudOperatingSystemVirtualizationSettings> baseImages = []
  }

  static class HuaweiCloudOperatingSystemVirtualizationSettings {
    BakeOptions.BaseImage baseImage
    List<HuaweiCloudVirtualizationSettings> virtualizationSettings = []
  }

  @AutoClone(style = AutoCloneStyle.SIMPLE)
  static class HuaweiCloudVirtualizationSettings {
    String region
    String instanceType
    String sourceImageId
    String sshUserName
  }

  @PostConstruct
  void init() {
    cloudProviderBakeHandlerRegistry.register(
      BakeRequest.CloudProviderType.huaweicloud, huaweicloudBakeHandler)
  }
}

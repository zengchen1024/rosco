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

package com.netflix.spinnaker.rosco.providers.huaweicloud

import com.netflix.spinnaker.rosco.api.Bake
import com.netflix.spinnaker.rosco.api.BakeOptions
import com.netflix.spinnaker.rosco.api.BakeRequest
import com.netflix.spinnaker.rosco.providers.CloudProviderBakeHandler
import com.netflix.spinnaker.rosco.providers.huaweicloud.config.RoscoHuaweiCloudConfiguration
import com.netflix.spinnaker.rosco.providers.util.ImageNameFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
public class HuaweiCloudBakeHandler extends CloudProviderBakeHandler {

  private static final String IMAGE_NAME_TOKEN = 'huaweicloud: Creating ecs image:'

  ImageNameFactory imageNameFactory = new ImageNameFactory()

  @Autowired
  RoscoHuaweiCloudConfiguration.HuaweiCloudBakeryDefaults huaweicloudBakeryDefaults

  @Override
  def getBakeryDefaults() {
    return huaweicloudBakeryDefaults
  }

  @Override
  BakeOptions getBakeOptions() {
    new BakeOptions(
      cloudProvider: BakeRequest.CloudProviderType.huaweicloud,
      baseImages: huaweicloudBakeryDefaults?.baseImages?.collect { it.baseImage }
    )
  }

  @Override
  String produceProviderSpecificBakeKeyComponent(String region, BakeRequest bakeRequest) {
    region
  }

  @Override
  def findVirtualizationSettings(String region, BakeRequest bakeRequest) {
    def huaweicloudOperatingSystemVirtualizationSettings = huaweicloudBakeryDefaults?.baseImages.find {
      it.baseImage.id == bakeRequest.base_os
    }

    if (!huaweicloudOperatingSystemVirtualizationSettings) {
      throw new IllegalArgumentException("No virtualization settings found for '$bakeRequest.base_os'.")
    }

    def huaweicloudVirtualizationSettings = huaweicloudOperatingSystemVirtualizationSettings?.virtualizationSettings.find {
      it.region == region
    }

    if (!huaweicloudVirtualizationSettings) {
      throw new IllegalArgumentException(
        "No virtualization settings found for region '$region' and operating system '$bakeRequest.base_os'.")
    }

    if (bakeRequest.base_ami) {
      huaweicloudVirtualizationSettings = huaweicloudVirtualizationSettings.clone()
      huaweicloudVirtualizationSettings.sourceImageId = bakeRequest.base_ami
    }

    return huaweicloudVirtualizationSettings
  }

  @Override
  Map buildParameterMap(String region, def huaweicloudVirtualizationSettings,
                        String imageName, BakeRequest bakeRequest, String appVersionStr) {
    def parameterMap = [
      huaweicloud_auth_url: huaweicloudBakeryDefaults.authUrl,
      huaweicloud_region: region,
      huaweicloud_ssh_username: huaweicloudVirtualizationSettings.sshUserName,
      huaweicloud_instance_type: huaweicloudVirtualizationSettings.instanceType,
      huaweicloud_source_image_id: huaweicloudVirtualizationSettings.sourceImageId,
      huaweicloud_image_name: imageName
    ]

    if (huaweicloudBakeryDefaults.username && huaweicloudBakeryDefaults.password) {
      parameterMap.huaweicloud_username = huaweicloudBakeryDefaults.username
      parameterMap.huaweicloud_password = huaweicloudBakeryDefaults.password
    }

    if (huaweicloudBakeryDefaults.domainName) {
      parameterMap.huaweicloud_domain_name = huaweicloudBakeryDefaults.domainName
    }

    if (huaweicloudBakeryDefaults.floatingIpPool) {
      parameterMap.huaweicloud_floating_ip_pool = huaweicloudBakeryDefaults.floatingIpPool
    }

    if (huaweicloudBakeryDefaults.networkId) {
      parameterMap.huaweicloud_network_id = huaweicloudBakeryDefaults.networkId
    }

    if (huaweicloudBakeryDefaults.insecure != null) {
      parameterMap.huaweicloud_insecure = huaweicloudBakeryDefaults.insecure
    }

    if (huaweicloudBakeryDefaults.securityGroups) {
      parameterMap.huaweicloud_security_groups = huaweicloudBakeryDefaults.securityGroups
    }

    if (huaweicloudBakeryDefaults.projectName) {
      parameterMap.huaweicloud_project_name = huaweicloudBakeryDefaults.projectName
    }

    if (bakeRequest.build_info_url) {
      parameterMap.build_info_url = bakeRequest.build_info_url
    }

    if (appVersionStr) {
      parameterMap.appversion = appVersionStr
    }

    return parameterMap
  }

  @Override
  String getTemplateFileName(BakeOptions.BaseImage baseImage) {
    return baseImage.templateFile ?: huaweicloudBakeryDefaults.templateFile
  }

  @Override
  Bake scrapeCompletedBakeResults(String region, String bakeId, String logsContent) {
    String imageName

    logsContent.eachLine { String line ->
      if (line =~ IMAGE_NAME_TOKEN) {
        imageName = line.split(" ").last()
      }
    }

    new Bake(id: bakeId, image_name: imageName)
  }

  @Override
  List<String> getMaskedPackerParameters() {
    ['huaweicloud_password']
  }
}

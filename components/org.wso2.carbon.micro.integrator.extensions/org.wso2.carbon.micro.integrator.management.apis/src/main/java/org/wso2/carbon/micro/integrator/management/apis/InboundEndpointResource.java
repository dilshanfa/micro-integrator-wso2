/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.wso2.carbon.micro.integrator.management.apis;

import org.apache.http.NameValuePair;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.inbound.InboundEndpoint;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wso2.carbon.inbound.endpoint.internal.http.api.APIResource;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class InboundEndpointResource extends APIResource {

    public InboundEndpointResource(String urlTemplate){
        super(urlTemplate);
    }

    @Override
    public Set<String> getMethods() {
        Set<String> methods = new HashSet<>();
        methods.add("GET");
        methods.add("POST");
        return methods;
    }

    @Override
    public boolean invoke(MessageContext messageContext) {

        buildMessage(messageContext);

        org.apache.axis2.context.MessageContext axis2MessageContext =
                ((Axis2MessageContext) messageContext).getAxis2MessageContext();

        List<NameValuePair> queryParameter = Utils.getQueryParameters(axis2MessageContext);

        // if query params exists retrieve data about specific inbound endpoint
        if (Objects.nonNull(queryParameter)) {
            for (NameValuePair nvPair : queryParameter) {
                if (nvPair.getName().equals("inboundEndpointName")) {
                    populateInboundEndpointData(messageContext, nvPair.getValue());
                    break;
                }
            }
        } else {
            populateInboundEndpointList(messageContext);
        }

        axis2MessageContext.removeProperty(Constants.NO_ENTITY_BODY);
        return true;
    }

    private void populateInboundEndpointList(MessageContext messageContext) {

        org.apache.axis2.context.MessageContext axis2MessageContext =
                ((Axis2MessageContext) messageContext).getAxis2MessageContext();

        SynapseConfiguration configuration = messageContext.getConfiguration();

        Collection<InboundEndpoint> inboundEndpoints = configuration.getInboundEndpoints();

        JSONObject jsonBody = new JSONObject();
        JSONArray inboundList = new JSONArray();
        jsonBody.put(Constants.COUNT, inboundEndpoints.size());
        jsonBody.put(Constants.LIST, inboundList);

        for (InboundEndpoint inboundEndpoint : inboundEndpoints) {

            JSONObject inboundObject = new JSONObject();

            inboundObject.put(Constants.NAME, inboundEndpoint.getName());
            inboundObject.put("protocol", inboundEndpoint.getProtocol());

            inboundList.put(inboundObject);
        }
        Utils.setJsonPayLoad(axis2MessageContext, jsonBody);
    }

    private void populateInboundEndpointData(MessageContext messageContext, String inboundEndpointName) {

        org.apache.axis2.context.MessageContext axis2MessageContext =
                ((Axis2MessageContext) messageContext).getAxis2MessageContext();

        JSONObject jsonBody = getInboundEndpointByName(messageContext, inboundEndpointName);

        if (Objects.nonNull(jsonBody)) {
            Utils.setJsonPayLoad(axis2MessageContext, jsonBody);
        } else {
            axis2MessageContext.setProperty(Constants.HTTP_STATUS_CODE, Constants.NOT_FOUND);
        }
    }

    private JSONObject getInboundEndpointByName(MessageContext messageContext, String inboundEndpointName) {

        SynapseConfiguration configuration = messageContext.getConfiguration();
        InboundEndpoint ep = configuration.getInboundEndpoint(inboundEndpointName);
        return convertInboundEndpointToOMElement(ep);
    }

    private JSONObject convertInboundEndpointToOMElement(InboundEndpoint inboundEndpoint) {

        if (Objects.isNull(inboundEndpoint)) {
            return null;
        }

        JSONObject inboundObject = new JSONObject();

        inboundObject.put(Constants.NAME, inboundEndpoint.getName());
        inboundObject.put("protocol", inboundEndpoint.getProtocol());

        String statisticState = inboundEndpoint.getAspectConfiguration().isStatisticsEnable() ? Constants.ENABLED : Constants.DISABLED;
        inboundObject.put(Constants.STATS, statisticState);

        String tracingState = inboundEndpoint.getAspectConfiguration().isTracingEnabled() ? Constants.ENABLED : Constants.DISABLED;
        inboundObject.put(Constants.TRACING, tracingState);

        JSONArray parameterListObject = new JSONArray();

        inboundObject.put("parameters", parameterListObject);

        Map<String, String> params = inboundEndpoint.getParametersMap();

        for (Map.Entry<String,String> param : params.entrySet()) {

            JSONObject paramObject = new JSONObject();

            paramObject.put(Constants.NAME, param.getKey());
            paramObject.put("value", param.getValue());

            parameterListObject.put(paramObject);
        }
        return inboundObject;
    }
}

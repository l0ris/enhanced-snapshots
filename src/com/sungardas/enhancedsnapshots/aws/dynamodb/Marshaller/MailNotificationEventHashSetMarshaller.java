package com.sungardas.enhancedsnapshots.aws.dynamodb.Marshaller;

import com.amazonaws.services.dynamodbv2.datamodeling.JsonMarshaller;
import com.sungardas.enhancedsnapshots.enumeration.MailNotificationEvent;

import java.util.Set;
import java.util.stream.Collectors;

public class MailNotificationEventHashSetMarshaller extends JsonMarshaller<Set> {
    @Override
    public String marshall(Set obj) {
        return super.marshall(obj);
    }

    @Override
    public Set unmarshall(Class<Set> clazz, String json) {
        return (Set) super.unmarshall(clazz, json).stream().map(e -> MailNotificationEvent.valueOf(e.toString())).collect(Collectors.toSet());
    }
}

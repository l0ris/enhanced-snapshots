package com.sungardas.enhancedsnapshots.dto.converter;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.MailConfigurationDocument;
import com.sungardas.enhancedsnapshots.dto.MailConfigurationDto;
import com.sungardas.enhancedsnapshots.service.CryptoService;
import org.springframework.beans.BeanUtils;

public class MailConfigurationDocumentConverter {
    private MailConfigurationDocumentConverter() {
    }

    public static MailConfigurationDocument toMailConfigurationDocument(MailConfigurationDto dto, CryptoService cryptoService, String configurationId, String oldPassword) {
        if (dto != null) {
            MailConfigurationDocument configurationDocument = new MailConfigurationDocument();
            BeanUtils.copyProperties(dto, configurationDocument);
            if (dto.getEvents() != null) {
                BeanUtils.copyProperties(dto.getEvents(), configurationDocument.getEvents());
            }
            if (configurationDocument.getPassword() != null) {
                configurationDocument.setPassword(cryptoService.encrypt(configurationId, configurationDocument.getPassword()));
            } else {
                configurationDocument.setPassword(oldPassword);
            }
            //DynamoDB does not support empty sets
            if (configurationDocument.getRecipients().isEmpty()) {
                configurationDocument.setRecipients(null);
            }
            return configurationDocument;
        } else {
            return null;
        }
    }

    public static MailConfigurationDto toMailConfigurationDto(MailConfigurationDocument document) {
        if (document != null) {
            MailConfigurationDto mailConfigurationDto = new MailConfigurationDto();
            BeanUtils.copyProperties(document, mailConfigurationDto);
            MailConfigurationDto.MailNotificationEvents events = new MailConfigurationDto.MailNotificationEvents();
            BeanUtils.copyProperties(document.getEvents(), events);
            mailConfigurationDto.setEvents(events);
            mailConfigurationDto.setPassword(null);
            return mailConfigurationDto;
        } else {
            return null;
        }
    }
}

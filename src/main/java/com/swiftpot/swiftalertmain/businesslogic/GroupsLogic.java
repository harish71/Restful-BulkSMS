package com.swiftpot.swiftalertmain.businesslogic;

import com.google.gson.Gson;
import com.swiftpot.swiftalertmain.db.model.GroupContactsDoc;
import com.swiftpot.swiftalertmain.db.model.GroupsDoc;
import com.swiftpot.swiftalertmain.db.model.UserDoc;
import com.swiftpot.swiftalertmain.helpers.CustomDateFormat;
import com.swiftpot.swiftalertmain.models.ErrorOutgoingPayload;
import com.swiftpot.swiftalertmain.models.OutgoingPayload;
import com.swiftpot.swiftalertmain.models.SuccessfulOutgoingPayload;
import com.swiftpot.swiftalertmain.repositories.GroupContactsDocRepository;
import com.swiftpot.swiftalertmain.repositories.GroupsDocRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @author Ace Programmer Rbk
 *         <Rodney Kwabena Boachie at [rodney@swiftpot.com,rbk.unlimited@gmail.com]> on
 *         02-Oct-16 @ 11:33 PM
 */
@Service
public class GroupsLogic {
    Logger log = LoggerFactory.getLogger(getClass());
    @Autowired
    Gson g;
    @Autowired
    GroupsDocRepository groupsDocRepository;
    @Autowired
    GroupContactsDocRepository groupContactsDocRepository;

    public OutgoingPayload getAllGroups(String userName) {
        log.info("GetAllGroups Registered To Username Request :" + userName);
        OutgoingPayload outgoingPayload;
        try {
            List<GroupsDoc> groupsDocList = groupsDocRepository.findAllByUserName(userName);
            log.info("Groups of user {} is {}", userName, g.toJson(groupsDocList));
            outgoingPayload = new SuccessfulOutgoingPayload(groupsDocList);
        } catch (Exception e) {
            outgoingPayload = new ErrorOutgoingPayload("Could Not Get Groups.Are you sure user exists?");
        }

        return outgoingPayload;
    }

    public OutgoingPayload updateOneGroup(GroupsDoc groupsDoc) {
        log.info("Update One GroupName Request : {}", g.toJson(groupsDoc));
        OutgoingPayload outgoingPayload;

        try {
            boolean isDocumentPresent = groupsDocRepository.exists(groupsDoc.getId());
            if (isDocumentPresent) {
                GroupsDoc groupsDocFinal = groupsDocRepository.save(groupsDoc);
                outgoingPayload = new SuccessfulOutgoingPayload(groupsDocFinal);
            } else {
                outgoingPayload = new ErrorOutgoingPayload("Entity does not exist,bro!!:)");
            }
        } catch (Exception e) {
            log.info("Exception cause : " + e.getCause().getMessage());
            outgoingPayload = new ErrorOutgoingPayload("Could Not Update entity with id : {}.I don't think id exists,bruh :)", groupsDoc.getId());
        }
        return outgoingPayload;
    }

    public OutgoingPayload deleteOneGroup(GroupsDoc groupsDoc) {
        log.info("Delete One GroupName Request : {}", g.toJson(groupsDoc));
        OutgoingPayload outgoingPayload;

        try {
            boolean isDocumentPresent = groupsDocRepository.exists(groupsDoc.getId());
            if (isDocumentPresent) {
                groupsDocRepository.delete(groupsDoc);

                /**
                 * initiate a new Thread inline to delete all contacts of group asynchrounously
                 */
                new Thread(() -> {
                    List<GroupContactsDoc> groupContactsDocsList = groupContactsDocRepository.findByGroupId(groupsDoc.getGroupId());
                    if(!(groupContactsDocsList.isEmpty())){
                        log.info("Number of GroupContacts To Delete = "+groupContactsDocsList.size());
                        groupContactsDocRepository.delete(groupContactsDocsList);
                        log.info(groupContactsDocsList.size()+" Contacts Deleted Successfully");
                    } else {
                        log.info("Number of GroupContacts To Delete = "+groupContactsDocsList.size());
                    }
                }).start();
                outgoingPayload = new SuccessfulOutgoingPayload("Deleted Successfully");
            } else {
                outgoingPayload = new ErrorOutgoingPayload("Entity does not exist,bro!!:)");
            }
        } catch (Exception e) {
            log.info("Exception cause : " + e.getCause().getMessage());
            outgoingPayload = new ErrorOutgoingPayload("Bro,I couldn't delete a Group with Id of {}.Are you drunk or what?:P =D", groupsDoc.getId());
        }
        return outgoingPayload;

    }

    public OutgoingPayload createOneGroup(GroupsDoc groupsDoc) {
        log.info("Create One GroupName Request : {}", g.toJson(groupsDoc));
        /**
         * generate 36char groupId,as its the largest uuid can gen, and save to db,this is used in both MessageReport and MessageReportDetailed for querying
         */
        String groupId = UUID.randomUUID().toString().toUpperCase().substring(0, 36);
        groupsDoc.setGroupId(groupId);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(CustomDateFormat.getDateFormat());
        String dateNow = simpleDateFormat.format(Date.from(Instant.now()));
        groupsDoc.setDateCreated(dateNow);

        OutgoingPayload outgoingPayload;

        String groupNameFromUser = groupsDoc.getGroupName();

        try{

        if(isGroupNamePresentAlready(groupNameFromUser)){
            //dont save document,return to user that it was present already
            outgoingPayload = new ErrorOutgoingPayload("Group Name exists already");
        }else{
            GroupsDoc newlyCreatedGroup = groupsDocRepository.save(groupsDoc);
            outgoingPayload = new SuccessfulOutgoingPayload("Created Successfully",newlyCreatedGroup);
        }
        }catch (Exception e ){
            outgoingPayload = new ErrorOutgoingPayload("Computers make mistakes too,bruh");
        }

        return outgoingPayload;
    }

    boolean isGroupNamePresentAlready(String groupName){
        boolean isGroupNamePresentAlrady = false;
        GroupsDoc groupsDoc = groupsDocRepository.findByGroupName(groupName);
        if(!(groupsDoc == null)){
            isGroupNamePresentAlrady = true;
        }else{
            //return false;
        }
        return isGroupNamePresentAlrady;
    }
}

package com.dotlabs.moneytransfer.service;

import com.dotlabs.moneytransfer.dto.request.AuthorizeTransferRequest;
import com.dotlabs.moneytransfer.dto.request.InitiateTransferRequest;
import com.dotlabs.moneytransfer.dto.request.TransferRequest;
import com.dotlabs.moneytransfer.dto.response.InitiateTransferResponse;
import com.dotlabs.moneytransfer.dto.response.TransferResponse;
import com.dotlabs.moneytransfer.entity.User;

public interface TransferService {
    TransferResponse processTransfer(TransferRequest request);
    InitiateTransferResponse initiateTransfer(InitiateTransferRequest request, User authenticatedUser);
    TransferResponse authorizeTransfer(AuthorizeTransferRequest request, User authenticatedUser);
}

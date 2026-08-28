package com.dotlabs.moneytransfer.service;

import com.dotlabs.moneytransfer.dto.request.TransferRequest;
import com.dotlabs.moneytransfer.dto.response.TransferResponse;

public interface TransferService {
    TransferResponse processTransfer(TransferRequest request);
}

var initialTransactions = true;

function addTransaction(transaction, type, allHtml) {
	var template = Handlebars.templates['transaction'];

	//abbreviate transaction variable
	var t = transaction;

	//set default transaction values
	t.filters        = "all";
	t.senderClass    = "standard";
	t.recipientClass = "standard";

	//set amount, sender, and recipient
	var sentReceived = "None";
	t.forgedBlock    = false;
	if (type == "my") {
		if (t.sentAmount !== undefined) {
			t.amount          = t.sentAmount;
			t.sender          = account ? account : "";
			t.senderClass     = "me";
			t.recipient       = t.account;
			t.filters        += " sent";

			sentReceived      = "Sent";
		} else if (t.receivedAmount !== undefined) {
			t.amount          = t.receivedAmount;
			t.recipient       = account ? account : "";
			t.recipientClass  = "me";
			t.sender          = t.account;
			t.filters        += " received";

			sentReceived      = "Received";
		} else {
			t.amount          = t.earnedAmount;
			t.recipient       = account ? account : "";
			t.recipientClass  = "me";
			t.sender          = t.index;
			t.filters        += " received";

			sentReceived      = "Received";
			t.forgedBlock     = true;
		}
	}

	if (t.sender == account) {
		t.senderClass  = "me";
		t.filters     += " sent";
	}

	if (t.recipient == account) {
		t.recipientClass  = "me";
		t.filters        += " received";
	}

	if (t.fee === undefined)
		t.fee = 0;

	//set sender and recipient URLs
	if (t.forgedBlock) {
		t.senderUrl = config.blockUrl;
		if (t.sender != "")
			t.senderUrl = t.senderUrl.replace('[blockId]', t.sender);
	} else {
		t.senderUrl = config.accountUrl;
		if (t.sender != "")
			t.senderUrl = t.senderUrl.replace('[accountId]', t.sender);
	}

	t.recipientUrl = config.accountUrl;
	if (t.recipient != "")
		t.recipientUrl = t.recipientUrl.replace('[accountId]', t.recipient);

	//set transaction URL
	t.transactionUrl = config.transactionUrl.replace('[transactionId]', t.id);

	//set timestamps
	t.timestampTransaction = false;
	if (t.transactionTimestamp !== undefined) {
		t.numericTimestamp     = t.transactionTimestamp;
		t.timestamp            = new Date(Date.UTC(2014, 02, 22, 22, 22, 0, 0) + t.transactionTimestamp * 1000);
		t.timestampTransaction = true;
	} else if (t.blockTimestamp !== undefined) {
		t.numericTimestamp     = t.blockTimestamp;
		t.timestamp            = new Date(Date.UTC(2014, 02, 22, 22, 22, 0, 0) + t.blockTimestamp * 1000);
	} else {
		t.numericTimestamp     = t.timestamp;
		t.timestamp            = new Date(Date.UTC(2014, 02, 22, 22, 22, 0, 0) + t.timestamp * 1000);
	}

	t.formattedTimestamp = moment(t.timestamp).format(config.dateTimeFormat);
	t.readableTimestamp  = moment(t.timestamp).fromNow();

	if (t.attachment !== undefined) {
		t.readableTimestamp += " " + t.attachment.alias;
	}

	//set number of confirmations
	t.numberOfConfirmations = t.numberOfConfirmations ? t.numberOfConfirmations : 0;

	if (t.numberOfConfirmations == 1)
		t.numberOfConfirmationsTitleText = t.numberOfConfirmations + ' ' + Language.get('labels.confirmation');
	else
		t.numberOfConfirmationsTitleText = t.numberOfConfirmations + ' ' + Language.get('labels.confirmations');

	t.numberOfConfirmationsFormatted = t.numberOfConfirmations;
	if (t.numberOfConfirmations > 100)
		t.numberOfConfirmationsFormatted = "100+";

	//set confirmed/unconfirmed filters
	if (t.numberOfConfirmations)
		t.filters += " confirmed";
	else
		t.filters += " unconfirmed";

	//get HTML markup for template and append it
	var html = template(t);

	if (initialTransactions)
	{
		if(typeof(allHtml) === undefined) {
			$('#' + type + '-transactions').append(html);
		} else {
			allHtml += html;
		}
	} else {
		if(typeof(allHtml) === undefined) {
			$('#' + type + '-transactions').prepend(html);
		} else {
			allHtml = html+allHtml;
		}
	}

	//add to "My Transactions" as well if transaction is being added to "All Transactions" and sender is current account
	if (type == "all" && t.senderClass == "me") {
		$('#my-transactions').prepend(html);
	}

	//add language text to new markup
	// Language.replaceText();

	if(typeof(allHtml) === undefined) {
		//get additional transaction data
		getAdditionalTransactionData(t.id);
	
		//update transactions counter
		updateCounter('transactions', type);

		allHtml = true;
	}

	return allHtml;

}

function addTransactions(transactions, type) {
	var ul = $("#"+type+"-transactions");
	var html = ul.html();
	if (type == "my")
		initialTransactions = true;

	for (t = 0; t < transactions.length; t++) {
		html = addTransaction(transactions[t], type, html);
	}

	ul.html(html);

	//update transactions counter
	updateCounter('transactions', type);

	reorderTransactions(type);

	checkNoItemsForSectionFilter('transactions', type + '-transactions-section');

	adjustPageTabContent();

	initialTransactions = false;
}

function reorderTransactions(type) {
	$('#'+type+'-transactions li').tsort('span.timestamp', {order: 'desc', attr: 'data-timestamp-numeric'});
}

function getAdditionalTransactionData(id) {
	Api.sendRequest('getTransaction', {transaction: id});
}

function formatDeadline(deadline) {
	if (deadline == 0)
		return "No deadline";

	var d, h, m;
	m = deadline % 60;
	h = ((deadline - m) / 60) % 24;
	d = (deadline - m - h * 60) / 1440;
	m = m > 0 ? (m + " minute" + (m > 1 ? "s" : "")) : "";
	h = h > 0 ? (h + " hour" + (h > 1 ? "s" : "")) : "";
	d = d > 0 ? (d + " day" + (d > 1 ? "s" : "")) : "";
	deadline = d + " " + h + " " + m;

	while (deadline.indexOf("  ") >= 0) {
		deadline = deadline.replace("  ", " ");
	}

	while (deadline.charAt(0) == " ") {
		deadline = deadline.substr(1);
	}

	while (deadline.charAt(deadline.length - 1) == " ") {
		deadline = deadline.substr(0, deadline.length - 1);
	}

	return deadline;
}

function formatTimestamp(timestamp) {
	return (new Date(Date.UTC(2014, 02, 22, 22, 22, 0, 0) + timestamp * 1000)).toLocaleString();
}

function updateTransactionConfirmations(amount) {
	if(typeof(amount) == undefined) { amount = 1; }
	$('#transactions ul.items li .confirmations').each(function(){
		var child = $(this).find('.number');
		var value = $(this).attr('title').split(" ")[0];
		var confirmations = parseInt(value,10)+amount;
		if(value < 100) {
			child.html(confirmations);
		} else {
			child.html('100+');
		}
		$(this).attr('title',confirmations+' Confirmations');
	});
}

function removeAllTransactions(type) {
	$('#' + type + '-transactions').html('');

	updateCounter('transactions', type);
}

function removeTransaction(transaction, type) {
	$('#' + type + '-transactions li.transaction-' + transaction.index).remove();

	updateCounter('transactions', type);
}

function removeTransactions(transactions, type) {
	var t;
	for (t = 0; t < transactions.length; t++) {
		removeTransaction(transactions[t], type);
	}
}

function sendMoney() {
	$('#modal-send .form').addClass('invisible');
	$('#modal-send .loading').fadeIn();	

	var secretPhrase = getSecretPhrase('send');

	var data = {
		recipient:    $('#send-recipient').val(),
		amount:       parseFloat($('#send-amount').val()),
		fee:          parseFloat($('#send-fee').val()),
		deadline:     parseInt($('#send-deadline').val()),
		secretPhrase: secretPhrase,
	};

	Api.sendUiRequest('sendMoney', data);
}

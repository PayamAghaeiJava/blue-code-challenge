package ir.rahgozin.wallet.infrastructure.api.resources;

import ir.rahgozin.wallet.application.wallet.command.DebitAccountCommand;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class DebitAccountRequestConverter implements Converter<DebitAccountRequest, DebitAccountCommand> {
    @Override
    public DebitAccountCommand convert(DebitAccountRequest source) {
        DebitAccountCommand command = new DebitAccountCommand();
        command.setRequestId(source.getRequestId());
        command.setAccountNumber(source.getAccountNumber());
        command.setAmount(source.getAmount());
        return command;
    }
}

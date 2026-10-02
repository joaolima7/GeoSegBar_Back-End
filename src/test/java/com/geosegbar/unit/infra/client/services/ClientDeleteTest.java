package com.geosegbar.unit.infra.client.services;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.anyLong;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.InstrumentTypeEntity;
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.exceptions.BusinessRuleException;
import com.geosegbar.infra.client.persistence.jpa.ClientRepository;
import com.geosegbar.infra.client.service.ClientService;
import com.geosegbar.infra.client.utils.ClientStatusChangeHandler;
import com.geosegbar.infra.file_storage.FileStorageService;
import com.geosegbar.infra.instrument_type.persistence.jpa.InstrumentTypeRepository;
import com.geosegbar.infra.question.persistence.jpa.QuestionRepository;
import com.geosegbar.infra.status.persistence.jpa.StatusRepository;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;
import com.geosegbar.infra.user.service.UserService;

/**
 * Excluir um cliente batia numa FK de questions e devolvia 500 com a mensagem
 * crua do Postgres. O catálogo do cliente — perguntas e tipos de instrumento —
 * não tem significado sem ele, e só pode ser apagado porque o guard já garantiu
 * que não há barragem: questionário, resposta e instrumento pendem todos de
 * barragem.
 */
@DisplayName("ClientService — exclusão")
class ClientDeleteTest extends BaseUnitTest {

    private static final Long CLIENT_ID = 15L;

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;
    @Mock
    private StatusRepository statusRepository;
    @Mock
    private ClientStatusChangeHandler statusChangeHandler;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private InstrumentTypeRepository instrumentTypeRepository;

    @InjectMocks
    private ClientService clientService;

    private void clientWithoutOperationalData() {
        ClientEntity client = new ClientEntity();
        client.setId(CLIENT_ID);
        lenient().when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        lenient().when(clientRepository.countDamsByClientId(CLIENT_ID)).thenReturn(0L);
        lenient().when(clientRepository.countDamPermissionsByClientId(CLIENT_ID)).thenReturn(0L);
        lenient().when(clientRepository.countUsersByClientId(CLIENT_ID)).thenReturn(0L);
    }

    private static QuestionEntity question(Long id) {
        QuestionEntity question = new QuestionEntity();
        question.setId(id);
        return question;
    }

    private static InstrumentTypeEntity instrumentType(Long id) {
        InstrumentTypeEntity type = new InstrumentTypeEntity();
        type.setId(id);
        return type;
    }

    @Test
    @DisplayName("apaga as perguntas do cliente antes de apagar o cliente")
    void deletesClientQuestions() {
        clientWithoutOperationalData();
        List<QuestionEntity> questions = List.of(question(1L), question(2L));
        when(questionRepository.findByClientId(CLIENT_ID)).thenReturn(questions);
        when(instrumentTypeRepository.findByClientIdOrderByNameAsc(CLIENT_ID)).thenReturn(List.of());

        clientService.deleteById(CLIENT_ID);

        verify(questionRepository).deleteAll(questions);
        verify(clientRepository).deleteById(CLIENT_ID);
    }

    @Test
    @DisplayName("apaga também os tipos de instrumento, que são do catálogo do cliente")
    void deletesClientInstrumentTypes() {
        clientWithoutOperationalData();
        when(questionRepository.findByClientId(CLIENT_ID)).thenReturn(List.of());
        List<InstrumentTypeEntity> types = List.of(instrumentType(7L));
        when(instrumentTypeRepository.findByClientIdOrderByNameAsc(CLIENT_ID)).thenReturn(types);

        clientService.deleteById(CLIENT_ID);

        verify(instrumentTypeRepository).deleteAll(types);
        verify(clientRepository).deleteById(CLIENT_ID);
    }

    @Test
    @DisplayName("cliente com barragem continua sendo recusado, sem tocar no catálogo")
    void stillRefusesClientWithDams() {
        ClientEntity client = new ClientEntity();
        client.setId(CLIENT_ID);
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        when(clientRepository.countDamsByClientId(CLIENT_ID)).thenReturn(3L);

        assertThatThrownBy(() -> clientService.deleteById(CLIENT_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("dependências");

        verify(questionRepository, never()).deleteAll(ArgumentMatchers.anyList());
        verify(instrumentTypeRepository, never()).deleteAll(ArgumentMatchers.anyList());
        verify(clientRepository, never()).deleteById(anyLong());
    }
}

package com.geosegbar.entities;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.geosegbar.common.objects_values.UserCreatorInfo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome é obrigatório!")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Email é obrigatório!")
    @Email(message = "Email inválido!")
    @Column(nullable = false, unique = true)
    private String email;

    @JsonProperty(access = Access.WRITE_ONLY)
    @NotBlank(message = "A senha não pode estar em branco!")
    @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres!")
    @Column(nullable = false)
    private String password;

    @Column(nullable = true, length = 11)
    private String phone;

    @NotNull(message = "O sexo deve ser informado!")
    @ManyToOne
    @JoinColumn(name = "sex_id", nullable = false)
    private SexEntity sex;

    @ManyToOne
    @JoinColumn(name = "status_id", nullable = false)
    private StatusEntity status;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    @NotNull(message = "A role deve ser informada!")
    private RoleEntity role;

    @Column(name = "is_first_access")
    private Boolean isFirstAccess = false;

    @ManyToMany
    @JoinTable(
            name = "user_client",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "client_id")
    )
    private Set<ClientEntity> clients = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<DamPermissionEntity> damPermissions = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private UserEntity createdBy;

    @JsonIgnore
    @OneToMany(mappedBy = "createdBy", fetch = FetchType.LAZY)
    private Set<UserEntity> createdUsers = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "createdBy", fetch = FetchType.LAZY)
    private Set<PSBFolderEntity> psbFoldersCreated = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "uploadedBy", fetch = FetchType.LAZY)
    private Set<PSBFileEntity> psbFilesUploaded = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "sharedBy", fetch = FetchType.LAZY)
    private Set<ShareFolderEntity> sharedFolders = new HashSet<>();

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnoreProperties("user")
    private AttributionsPermissionEntity attributionsPermission;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnoreProperties("user")
    private DocumentationPermissionEntity documentationPermission;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnoreProperties("user")
    private InstrumentationPermissionEntity instrumentationPermission;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnoreProperties("user")
    private RoutineInspectionPermissionEntity routineInspectionPermission;

    @JsonProperty("createdBy")
    public Object getCreatedByInfo() {
        if (this.createdBy == null) {
            return null;
        }
        return new UserCreatorInfo(createdBy.getId(), createdBy.getName(), createdBy.getEmail());
    }
}

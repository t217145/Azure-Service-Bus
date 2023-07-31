# Azure Service Bus Demo - Auto provising by Terraform

#### Prerequisite
    1. install Azure CLI
    2. download this git repo
    3. open PowerShell console and change directory to the downloaded folder

#### Procedure
1. Login to Azure and get subscription detail by run following command
`az login`

2. initial the terraform workspace
`./terraform init`

3. execute the terraform, with auto approve all
`./terraform apply -auto-approve`

4. After execute success, export those variable on screen
`./setup_env.ps1`

![Deployment success](/Stream/asb-demo-terraform/images/deployed.PNG?raw=true "Deployment success")

5. Set the corresponding variable in WebApps Application variables or setup in local environment

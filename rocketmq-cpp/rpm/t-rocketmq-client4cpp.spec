##############################################################
# http://twiki.corp.alimama.com/twiki/bin/view/Alimm_OPS/RPM #
# http://www.rpm.org/max-rpm/ch-rpm-inside.html              #
##############################################################
%define PROJECT_ROOT $RPM_BUILD_ROOT/../../..

Name: t-rocketmq-client4cpp
Version:1.0.0
Release: %(echo $RELEASE)%{?dist}
URL: %{_svn_path}

Summary: Project rocketmq-client4cpp
Group: Alibaba middleware Group
License: Copyright alibaba-inc Group

AutoReqProv: no

%description
This is the t-rocketmq-client4cpp rpm spec.

%build
cd %{PROJECT_ROOT}
make

# prepare your files
%install

# create dirs
mkdir -p $RPM_BUILD_ROOT%{_prefix}/rocketmq-client4cpp
mkdir -p $RPM_BUILD_ROOT%{_prefix}/rocketmq-client4cpp/include
mkdir -p $RPM_BUILD_ROOT%{_prefix}/rocketmq-client4cpp/lib

# copy files
cp -f %{PROJECT_ROOT}/bin/librocketmq.a      $RPM_BUILD_ROOT%{_prefix}/rocketmq-client4cpp/lib
cp -f %{PROJECT_ROOT}/bin/librocketmq.so     $RPM_BUILD_ROOT%{_prefix}/rocketmq-client4cpp/lib
cp -rf %{PROJECT_ROOT}/include/*             $RPM_BUILD_ROOT%{_prefix}/rocketmq-client4cpp/include
cp -f %{PROJECT_ROOT}/changelog              $RPM_BUILD_ROOT%{_prefix}/rocketmq-client4cpp/

# pre install
%pre

# post install
%post

# package infomation
%files
# set file attribute here
%defattr(-, admin, admin, 0755)
%{_prefix}/rocketmq-client4cpp

%define debug_package %{nil}
%define __os_install_post %{nil}

